package info.gabrielszabo.ublog.service;

import java.io.File;

import info.gabrielszabo.ublog.config.Config;

public class ContentService {

    private final File blogFolder;

    public ContentService() {
        blogFolder = new File(Config.TARGET_FOLDER.value());
    }

    public boolean createTopic(String topic) {
        if (topic == null || topic.isEmpty() || topic.equalsIgnoreCase("new")) {
            return false;
        }
        File topicFolder = new File(blogFolder, topic);
        if (topicFolder.exists()) {
            return false;
        }
        return topicFolder.mkdirs();
    }
    
}
