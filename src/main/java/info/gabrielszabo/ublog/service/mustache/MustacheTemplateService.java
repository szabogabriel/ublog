package info.gabrielszabo.ublog.service.mustache;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.jmtemplate.Template;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.service.BlogContext;
import info.gabrielszabo.ublog.service.TemplateService;

public class MustacheTemplateService implements TemplateService {

    private Map<String, Template> templateCache = new HashMap<>();

    private Boolean cachingEnabled = Boolean.parseBoolean(Config.MUSTACHE_TEMPLATES_CACHING_ENABLED.value());

    @Override
    public String render(String template, BlogContext context) {
        String ret = "";
        if (template == null) {
            throw new IllegalArgumentException("Template name cannot be null");
        }

        Template t = getFromCache(template);

        if (t == null) {
            File templateFolder = new File(Config.MUSTACHE_TEMPLATES_FOLDER.value());

            if (!templateFolder.exists() || !templateFolder.isDirectory()) {
                throw new IllegalStateException(
                        "Template folder does not exist or is not a directory: " + templateFolder.getAbsolutePath());
            }

            t = new Template(templateFolder, template);

            cacheTemplate(template, t);
        }
        ret = t.render(((MustacheBlogContext)context).getContextData());
        return ret;
    }

    private void cacheTemplate(String templateName, Template template) {
        if (cachingEnabled) {
            templateCache.put(templateName, template);
        }
    }

    private Template getFromCache(String template) {
        if (cachingEnabled && templateCache.containsKey(template)) {
            return templateCache.get(template);
        } else {
            return null;
        }
    }

}
