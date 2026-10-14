package info.gabrielszabo.ublog.service.template;

import info.gabrielszabo.ublog.service.blog.BlogContext;

public interface TemplateService {

    String render(String template, BlogContext context);

}
