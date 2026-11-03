package info.gabrielszabo.ublog.service.requestHandlers;

import info.gabrielszabo.ublog.config.Config;
import info.gabrielszabo.ublog.server.http.HttpRequest;

final class SiteBaseUrl {

    private SiteBaseUrl() {
    }

    /** Configured site.base.url, else derived from the request; no trailing slash. */
    static String resolve(HttpRequest request) {
        String configured = Config.SITE_BASE_URL.value();
        String base;
        if (configured != null && !configured.isBlank()) {
            base = configured.trim();
        } else {
            String forwarded = request.getHeader("X-Forwarded-Proto");
            boolean secure = "https".equalsIgnoreCase(forwarded)
                    || Boolean.parseBoolean(Config.SESSION_COOKIE_SECURE.value());
            String host = request.getHost() == null ? "localhost" : request.getHost();
            base = (secure ? "https://" : "http://") + host;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
