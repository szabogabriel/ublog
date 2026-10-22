package info.gabrielszabo.ublog.service.template;

import java.util.Properties;

import info.gabrielszabo.ublog.service.blog.BlogContext;

public interface TemplateService {

    String render(String template, BlogContext context);

    Properties getTemplateProperties();

    void saveTemplateProperties(Properties properties);

}
