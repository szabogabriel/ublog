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
import info.gabrielszabo.ublog.utils.StringCache;

public class contentCache implements TemplateService {

    private static final String THEME_PROPERTIES_FILE = "theme.properties";

    private final ConcurrentMap<String, Template> templateCache = new ConcurrentHashMap<>();

    private final Boolean cachingEnabled = Boolean.valueOf(Config.MUSTACHE_TEMPLATES_CACHING_ENABLED.value());

    private final File templateFolder;

    private final Map<String, Object> stringsMap = new ConcurrentHashMap<>();

    private final StringCache<String> contentCache;
    private final Boolean contentCachingEnabled = Boolean.valueOf(Config.MUSTACHE_CONTENT_CACHING_ENABLED.value());

    public contentCache() {
        this.templateFolder = new File(Config.MUSTACHE_TEMPLATES_FOLDER.value());
        this.contentCache = new StringCache<>(Integer.parseInt(Config.MUSTACHE_CONTENT_CACHE_SIZE_KB.value()) * 1024);
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
        Map<String, Object> contextData = ((MustacheBlogContext) context).getContextData();
        String cacheKey = template + contextData.hashCode();

        if (contentCachingEnabled) {
            ret = contentCache.getCachedContent(cacheKey);
            if (!"".equals(ret)) {
                return ret;
            }
        }

        Template templateInstance = getTemplate(template);
        synchronized (templateInstance) {
            contextData.putAll(stringsMap);
            ret = templateInstance.render(contextData);
            if (contentCachingEnabled) {
                contentCache.cacheContent(cacheKey, ret);
            }
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
