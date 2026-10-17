package info.gabrielszabo.ublog.service.template.mustache;

import java.io.File;
import java.io.FileInputStream;
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

    private final ConcurrentMap<String, Template> templateCache = new ConcurrentHashMap<>();

    private final Boolean cachingEnabled = Boolean.valueOf(Config.MUSTACHE_TEMPLATES_CACHING_ENABLED.value());

    private final File templateFolder;

    private final Map<String, Object> stringsMap = new ConcurrentHashMap<>();

    public MustacheTemplateService() {
        this.templateFolder = new File(Config.MUSTACHE_TEMPLATES_FOLDER.value());
        File stringsFile = new File(Config.MUSTACHE_TEMPLATES_FOLDER.value(), "strings.properties");
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

}
