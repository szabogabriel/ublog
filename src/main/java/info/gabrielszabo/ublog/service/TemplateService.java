package info.gabrielszabo.ublog.service;

public interface TemplateService {

    String render(String template, BlogContext context);

}
