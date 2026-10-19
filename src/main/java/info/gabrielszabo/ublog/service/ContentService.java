package info.gabrielszabo.ublog.service;

import java.util.regex.Pattern;
import java.io.File;

import info.gabrielszabo.ublog.config.Config;

public class ContentService {

    private final File blogFolder;
    private final Pattern topicNamePattern;

    public ContentService() {
        blogFolder = new File(Config.TARGET_FOLDER.value());
        topicNamePattern = Pattern.compile("^[a-zA-Z0-9_-]+$");
    }

    public boolean createTopic(String topic) {
        if (topic == null || topic.isEmpty() || topic.equalsIgnoreCase("new") || !topicNamePattern.matcher(topic).matches()) {
            return false;
        }
        File topicFolder = new File(blogFolder, topic);
        if (topicFolder.exists()) {
            return false;
        }
        return topicFolder.mkdirs();
    }
    
}
