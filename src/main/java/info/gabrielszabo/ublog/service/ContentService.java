package info.gabrielszabo.ublog.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.regex.Pattern;

import info.gabrielszabo.ublog.config.Config;

public class ContentService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern UNLISTED_FILE_NAME = Pattern.compile("^[0-9a-f]{32}$");

    private final File blogFolder;
    private final Pattern topicNamePattern;
    private final Pattern entryNamePattern;

    public ContentService() {
        blogFolder = new File(Config.BLOG_FOLDER.value());
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

    public boolean createEntry(String topic, String entry, String content, File[] images, boolean unlisted) {
        if (!isEntryLocation(topic) || entry == null || !entryNamePattern.matcher(entry.trim()).matches()
                || content == null) {
            return false;
        }
        entry = entry.trim().replaceAll(" +", "_");
        // A root entry sharing a topic's name would be unreachable.
        if (isRoot(topic) && new File(blogFolder, entry).exists()) {
            return false;
        }
        File entryFolder = entryFolder(topic);
        File entryFile = new File(entryFolder, entry + ".md");

        if (unlisted) {
            String unlistedFileName = generateUnlistedFileName(entryFolder);
            File unlistedEntryFile = new File(entryFolder, unlistedFileName);
            try {
                Files.writeString(unlistedEntryFile.toPath(), content, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW);
                Files.createSymbolicLink(entryFile.toPath(), Path.of(unlistedFileName));
                return true;
            } catch (IOException e) {
                try {
                    Files.deleteIfExists(unlistedEntryFile.toPath());
                } catch (IOException ignored) {
                    // Best effort cleanup.
                }
                return false;
            }
        } else {
            try {
                Files.writeString(entryFile.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /**
     * Returns the raw markdown of an existing entry, or null if it does not exist.
     * Unlisted entries are only returned when includeUnlisted is set.
     */
    public String getEntryContent(String topic, String entry, boolean includeUnlisted) {
        File entryFile = existingEntryFile(topic, entry);
        if (entryFile == null || (!includeUnlisted && isUnlisted(entryFile))) {
            return null;
        }
        try {
            return Files.readString(contentPath(entryFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** True if the entry exists and is unlisted. */
    public boolean isEntryUnlisted(String topic, String entry) {
        File entryFile = existingEntryFile(topic, entry);
        return entryFile != null && isUnlisted(entryFile);
    }

    public boolean updateEntry(String topic, String entry, String content, boolean unlisted) {
        File entryFile = existingEntryFile(topic, entry);
        if (entryFile == null || content == null) {
            return false;
        }
        Path temporaryFile = null;
        try {
            Path entryPath = entryFile.toPath();
            Path contentPath = contentPath(entryFile);
            temporaryFile = Files.createTempFile(entryPath.getParent(), "." + entryFile.getName() + ".", ".tmp");
            Files.writeString(temporaryFile, content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(temporaryFile, contentPath, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            temporaryFile = null;

            boolean currentlyUnlisted = isUnlisted(entryFile);
            if (unlisted && !currentlyUnlisted) {
                Path randomPath = entryPath.resolveSibling(generateUnlistedFileName(entryFile.getParentFile()));
                Files.move(entryPath, randomPath, StandardCopyOption.ATOMIC_MOVE);
                Files.createSymbolicLink(entryPath, randomPath.getFileName());
            } else if (!unlisted && currentlyUnlisted) {
                // Renaming over the link replaces the link itself.
                Files.move(contentPath, entryPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Best effort cleanup.
                }
            }
        }
    }

    // Single place to change when a different deletion strategy (e.g. archiving) is
    // needed.
    protected boolean deleteEntry(File entryFile) {
        try {
            Path contentPath = contentPath(entryFile);
            Files.delete(entryFile.toPath());
            if (!contentPath.equals(entryFile.toPath())) {
                Files.deleteIfExists(contentPath);
            }
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
        Path entryPath = entryFile.toPath();
        if (Files.isSymbolicLink(entryPath)) {
            return unlistedTarget(entryPath) != null ? entryFile : null;
        }
        return entryFile.isFile() ? entryFile : null;
    }

    private boolean isUnlisted(File entryFile) {
        return Files.isSymbolicLink(entryFile.toPath());
    }

    /** The file holding the entry's markdown: the entry itself, or the link target if unlisted. */
    private Path contentPath(File entryFile) {
        Path entryPath = entryFile.toPath();
        Path target = unlistedTarget(entryPath);
        return target != null ? target : entryPath;
    }

    /** The target of an unlisted entry link, or null if the path is not a valid unlisted link. */
    private Path unlistedTarget(Path link) {
        try {
            if (!Files.isSymbolicLink(link)) {
                return null;
            }
            Path linkTarget = Files.readSymbolicLink(link);
            if (!UNLISTED_FILE_NAME.matcher(linkTarget.toString()).matches()) {
                return null;
            }
            Path target = link.resolveSibling(linkTarget);
            return Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) ? target : null;
        } catch (IOException e) {
            return null;
        }
    }

    private String generateUnlistedFileName(File targetFolder) {
        byte[] randomBytes = new byte[16];
        String ret;
        do {
            RANDOM.nextBytes(randomBytes);
            ret = HexFormat.of().formatHex(randomBytes);
        } while (isFilePresentInFolder(targetFolder, ret));
        return ret;
    }

    private boolean isFilePresentInFolder(File folder, String fileName) {
        if (folder == null || fileName == null) {
            return false;
        }
        File file = new File(folder, fileName);
        return file.isFile() && !Files.isSymbolicLink(file.toPath());
    }

}
