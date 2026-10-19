package info.gabrielszabo.ublog.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        return topic != null && topicNamePattern.matcher(topic).matches()
                && new File(blogFolder, topic).isDirectory();
    }

    public boolean createEntry(String topic, String entry, String content, File[] images) {
        if (!topicExists(topic) || entry == null || !entryNamePattern.matcher(entry.trim()).matches()
                || content == null) {
            return false;
        }
        entry = entry.trim().replaceAll(" +", "_");
        File entryFile = new File(new File(blogFolder, topic), entry + ".md");
        try {
            Files.writeString(entryFile.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
}
