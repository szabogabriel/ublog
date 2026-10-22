package info.gabrielszabo.ublog.service.template.mustache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.jmtemplate.Template;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class MustacheTemplateService implements TemplateService {

    private static final String THEME_PROPERTIES_FILE = "theme.properties";

    private final ConcurrentMap<String, Template> templateCache = new ConcurrentHashMap<>();

    private final Boolean cachingEnabled = Boolean.valueOf(Config.MUSTACHE_TEMPLATES_CACHING_ENABLED.value());

    private final File templateFolder;

    private final Map<String, Object> stringsMap = new ConcurrentHashMap<>();

    public MustacheTemplateService() {
        this.templateFolder = new File(Config.MUSTACHE_TEMPLATES_FOLDER.value());
        File stringsFile = getThemesFile();
        if (stringsFile.exists() && stringsFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(stringsFile)) {
                Properties props = new Properties();
                props.load(fis);
                for (String key : props.stringPropertyNames()) {
                    stringsMap.put(key, props.getProperty(key));
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load strings properties", e);
            }
        }
    }

    @Override
    public String render(String template, BlogContext context) {
        String ret;
        if (template == null) {
            throw new IllegalArgumentException("Template name cannot be null");
        }

        Template templateInstance = getTemplate(template);
        synchronized (templateInstance) {
            Map<String, Object> contextData = ((MustacheBlogContext) context).getContextData();
            contextData.putAll(stringsMap);
            ret = templateInstance.render(contextData);
        }
        return ret;
    }

    private Template getTemplate(String templateName) {
        if (!cachingEnabled) {
            return createTemplate(templateName);
        }
        return templateCache.computeIfAbsent(templateName, this::createTemplate);
    }

    private Template createTemplate(String templateName) {
        if (!templateFolder.exists() || !templateFolder.isDirectory()) {
            throw new IllegalStateException(
                    "Template folder does not exist or is not a directory: " + templateFolder.getAbsolutePath());
        }
        return new Template(templateFolder, templateName, Boolean.valueOf(Config.MUSTACHE_TEMPLATE_UPDATE.value()));
    }

    @Override
    public Properties getTemplateProperties() {
        File stringsFile = getThemesFile();
        Properties props = new Properties();
        if (stringsFile.exists() && stringsFile.isFile()) {
            try (FileInputStream fis = new FileInputStream(stringsFile)) {
                props.load(fis);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load template properties", e);
            }
        }
        return props;
    }

    @Override
    public void saveTemplateProperties(Properties properties) {
        File stringsFile = getThemesFile();
        try (FileOutputStream fos = new FileOutputStream(stringsFile)) {
            properties.store(fos, null);
            stringsMap.clear();
            for (String key : properties.stringPropertyNames()) {
                stringsMap.put(key, properties.getProperty(key));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save template properties", e);
        }
    }

    private File getThemesFile() {
        File stringsFile = new File(Config.MUSTACHE_TEMPLATES_FOLDER.value(), THEME_PROPERTIES_FILE);
        return stringsFile;
    }

}
