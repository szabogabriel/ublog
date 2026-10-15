package info.gabrielszabo.ublog.service.template.mustache;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.jmtemplate.Template;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.template.TemplateService;

public class MustacheTemplateService implements TemplateService {

    private final ConcurrentMap<String, Template> templateCache = new ConcurrentHashMap<>();

    private final Boolean cachingEnabled = Boolean.valueOf(Config.MUSTACHE_TEMPLATES_CACHING_ENABLED.value());

    @Override
    public String render(String template, BlogContext context) {
        String ret;
        if (template == null) {
            throw new IllegalArgumentException("Template name cannot be null");
        }

        Template templateInstance = getTemplate(template);
        synchronized (templateInstance) {
            ret = templateInstance.render(((MustacheBlogContext) context).getContextData());
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
        File templateFolder = new File(Config.MUSTACHE_TEMPLATES_FOLDER.value());
        if (!templateFolder.exists() || !templateFolder.isDirectory()) {
            throw new IllegalStateException(
                    "Template folder does not exist or is not a directory: " + templateFolder.getAbsolutePath());
        }
        return new Template(templateFolder, templateName);
    }

}
