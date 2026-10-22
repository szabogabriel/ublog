package info.gabrielszabo.ublog.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import info.gabrielszabo.jdi.services.ServiceFactory;
import info.gabrielszabo.ublog.content.ContentProvider;
import info.gabrielszabo.ublog.content.MarkdownContentProvider;
import info.gabrielszabo.ublog.markdown.CommonmarkRenderingService;
import info.gabrielszabo.ublog.markdown.MarkdownRenderingService;
import info.gabrielszabo.ublog.server.Server;
import info.gabrielszabo.ublog.server.http.HttpRequestHandler;
import info.gabrielszabo.ublog.server.socket.PooledSocketHandler;
import info.gabrielszabo.ublog.server.socket.SocketHandler;
import info.gabrielszabo.ublog.service.ContentService;
import info.gabrielszabo.ublog.service.LoginService;
import info.gabrielszabo.ublog.service.RequestHandlerService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.log.ConsoleLogService;
import info.gabrielszabo.ublog.service.log.LogService;
import info.gabrielszabo.ublog.service.template.TemplateService;
import info.gabrielszabo.ublog.service.template.mustache.MustacheBlogContextFactory;
import info.gabrielszabo.ublog.service.template.mustache.MustacheTemplateService;

public enum Config {

    MUSTACHE_TEMPLATES_CACHING_ENABLED("mustache.templates.caching.enabled", "true"),
    MUSTACHE_TEMPLATES_FOLDER("mustache.templates.folder", "./template"),
    MUSTACHE_TEMPLATE_UPDATE("mustache.template.update", "false"),

    SERVER_PORT("server.port", "8080"),

    SESSION_TIMEOUT("session.timeout", "60"),
    SESSION_COOKIE_SECURE("session.cookie.secure", "false"),

    SOCKET_HANDLER_THREAD_POOL_SIZE("socket.handler.thread.pool.size", "10"),
    SOCKET_HANDLER_QUEUE_CAPACITY("socket.handler.queue.capacity", "100"),

    BLOG_FOLDER("blog.folder", "./blogFolder"),
    IMAGE_FOLDER("image.folder", "./imageFolder"),
    USER_FOLDER("user.folder", "./userFolder"),

    SERVICE_BLOG_CONTEXT_FACTORY(ServiceFactory.PREFIX_IMPL + BlogContextFactory.class.getName(), MustacheBlogContextFactory.class.getName()),
    SERVICE_CONTENT_HANDLER(ServiceFactory.PREFIX_IMPL + ContentProvider.class.getName(), MarkdownContentProvider.class.getName()),
    SERVICE_CONTENT_SERVICE(ServiceFactory.PREFIX_IMPL + ContentService.class.getName(), ContentService.class.getName()),
    SERVICE_HTTP_SERVER(ServiceFactory.PREFIX_IMPL + Server.class.getName(), Server.class.getName()),
    SERVICE_LOG(ServiceFactory.PREFIX_IMPL + LogService.class.getName(), ConsoleLogService.class.getName()),
    SERVICE_LOGIN(ServiceFactory.PREFIX_IMPL + LoginService.class.getName(), LoginService.class.getName()),
    SERVICE_MARKDOWN_RENDERING(ServiceFactory.PREFIX_IMPL + MarkdownRenderingService.class.getName(), CommonmarkRenderingService.class.getName()),
    SERVICE_REQUEST_HANDLER(ServiceFactory.PREFIX_IMPL + HttpRequestHandler.class.getName(), RequestHandlerService.class.getName()),
    SERVICE_SOCKET_HANDLER(ServiceFactory.PREFIX_IMPL + SocketHandler.class.getName(), PooledSocketHandler.class.getName()),
    SERVICE_TEMPLATE_SERVICE(ServiceFactory.PREFIX_IMPL + TemplateService.class.getName(), MustacheTemplateService.class.getName()),
    SERVICE_USER_SERVICE(ServiceFactory.PREFIX_IMPL + UserService.class.getName(), UserService.class.getName()),

    ;

    private static final String PROPERTIES_FILE = "app.properties";
    private static Properties props = null;

    private final String key;
    private final String defaultValue;

    Config(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public String value() {
        String ret = getValue(key);

        if (ret == null) {
            ret = defaultValue;
        }

        return ret;
    }

    public static String getValue(String key) {
        String envValue = System.getenv(key.toUpperCase().replace('.', '_'));
        if (envValue != null) {
            return envValue;
        }

        if (props == null) {
            loadProperties();
        }

        if (props != null && props.containsKey(key)) {
            return props.getProperty(key);
        }

        return null;
    }

    private static void loadProperties() {
        synchronized (Config.class) {
            if (props == null) {
                File propertiesFile = new File(PROPERTIES_FILE);

                if (propertiesFile.exists()) {
                    try (InputStream input = new FileInputStream(propertiesFile)) {
                        props = new Properties();
                        props.load(input);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        throw new RuntimeException("Failed to read config properties", ex);
                    }
                } else {
                    System.out.println(
                            "Properties file not found: " + PROPERTIES_FILE
                                    + ", using default values where applicable.");
                    props = new Properties(); // Initialize empty properties
                }
            }
        }
    }

}
