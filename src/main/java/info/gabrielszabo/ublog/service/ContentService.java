package info.gabrielszabo.ublog.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

import info.gabrielszabo.ublog.config.Config;

public class ContentService {

    private final File blogFolder;
    private final Pattern topicNamePattern;
    private final Pattern entryNamePattern;

    public ContentService() {
        blogFolder = new File(Config.TARGET_FOLDER.value());
        topicNamePattern = Pattern.compile("^[a-zA-Z0-9_ -]+$");
        entryNamePattern = Pattern.compile("^[a-zA-Z0-9_ -]+$");
    }

    public boolean createTopic(String topic) {
        if (topic == null || topic.isBlank() || !topicNamePattern.matcher(topic.trim()).matches()) {
            return false;
        }
        topic = topic.trim().replaceAll(" +", "_");
        if (topic.equalsIgnoreCase("new")) {
            return false;
        }
        File topicFolder = new File(blogFolder, topic);
        if (topicFolder.exists()) {
            return false;
        }
        return topicFolder.mkdirs();
    }

    public boolean topicExists(String topic) {
        if (topic == null || !topicNamePattern.matcher(topic).matches()) {
            return false;
        }
        Path topicPath = new File(blogFolder, topic).toPath();
        return !Files.isSymbolicLink(topicPath) && Files.isDirectory(topicPath);
    }

    /** An empty topic denotes the blog root. */
    public static boolean isRoot(String topic) {
        return topic != null && topic.isEmpty();
    }

    private boolean isEntryLocation(String topic) {
        return isRoot(topic) || topicExists(topic);
    }

    private File entryFolder(String topic) {
        return isRoot(topic) ? blogFolder : new File(blogFolder, topic);
    }

    public boolean createEntry(String topic, String entry, String content, File[] images) {
        if (!isEntryLocation(topic) || entry == null || !entryNamePattern.matcher(entry.trim()).matches()
                || content == null) {
            return false;
        }
        entry = entry.trim().replaceAll(" +", "_");
        // A root entry sharing a topic's name would be unreachable.
        if (isRoot(topic) && new File(blogFolder, entry).exists()) {
            return false;
        }
        File entryFile = new File(entryFolder(topic), entry + ".md");
        try {
            Files.writeString(entryFile.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Returns the raw markdown of an existing entry, or null if it does not exist. */
    public String getEntryContent(String topic, String entry) {
        File entryFile = existingEntryFile(topic, entry);
        if (entryFile == null) {
            return null;
        }
        try {
            return Files.readString(entryFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean updateEntry(String topic, String entry, String content) {
        File entryFile = existingEntryFile(topic, entry);
        if (entryFile == null || content == null) {
            return false;
        }
        Path temporaryFile = null;
        boolean originalRemoved = false;
        try {
            Path entryPath = entryFile.toPath();
            temporaryFile = Files.createTempFile(entryPath.getParent(), "." + entryFile.getName() + ".", ".tmp");
            Files.writeString(temporaryFile, content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            if (!deleteEntry(entryFile)) {
                return false;
            }
            originalRemoved = true;
            Files.move(temporaryFile, entryPath);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (temporaryFile != null && !originalRemoved) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Best effort cleanup before the original is removed.
                }
            }
        }
    }

    // Single place to change when a different deletion strategy (e.g. archiving) is needed.
    protected boolean deleteEntry(File entryFile) {
        try {
            Files.delete(entryFile.toPath());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteEntry(String topic, String entry) {
        File entryFile = existingEntryFile(topic, entry);
        return entryFile != null && deleteEntry(entryFile);
    }

    private File existingEntryFile(String topic, String entry) {
        if (!isEntryLocation(topic) || entry == null || !entryNamePattern.matcher(entry).matches()) {
            return null;
        }
        File entryFile = new File(entryFolder(topic), entry + ".md");
        return entryFile.isFile() && !Files.isSymbolicLink(entryFile.toPath()) ? entryFile : null;
    }

}
