package info.gabrielszabo.ublog.server.http;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequest {

    private HttpMethods method;
    private String path;
    private QueryString queryString;
    private String httpVersion;
    private String host;
    private int contentLength;
    private String contentType;
    private MimeType mimeType;
    private Map<String, String> cookies = new HashMap<>();

    private Map<String, String> headers;

    private String body;

    public HttpRequest() {
        headers = new LinkedHashMap<>();
    }

    public HttpRequest(String rawRequest) {
        this();
        // Split headers and body at the first empty line
        String[] sections = rawRequest.split("\r\n\r\n", 2);
        String headerSection = sections.length > 0 ? sections[0] : "";
        String bodySection = sections.length > 1 ? sections[1] : "";

        String[] lines = headerSection.split("\r\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i == 0) {
                // First line: Request line
                String[] parts = line.split(" ");
                method = HttpMethods.fromString(parts[0]);
                httpVersion = parts.length > 2 ? parts[2] : "";
                String pathWithQueryString = parts.length > 1 ? parts[1] : "";
                String[] pathParts = pathWithQueryString.split("\\?", 2);
                path = pathParts[0];
                if (pathParts.length > 1) {
                    queryString = new QueryString(pathParts[1]);
                }
                continue;
            }

            String lower = line.toLowerCase();
            if (lower.startsWith("host:")) {
                host = line.substring(5).trim();
            }
            if (lower.startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(line.substring(16).trim());
                } catch (NumberFormatException e) {
                    contentLength = 0;
                }
            }
            if (lower.startsWith("content-type:")) {
                contentType = line.substring(14).trim();
                mimeType = MimeType.fromString(contentType);
            }
            if (lower.startsWith("cookie:")) {
                String[] cookiePairs = line.substring(7).trim().split(";");
                for (String cookie : cookiePairs) {
                    String[] cookieParts = cookie.split("=", 2);
                    if (cookieParts.length == 2) {
                        this.cookies.put(cookieParts[0].trim(), cookieParts[1].trim());
                    } else {
                        this.cookies.put(cookieParts[0].trim(), "");
                    }
                }
            }

            int colonIndex = line.indexOf(":");
            if (colonIndex != -1) {
                String name = line.substring(0, colonIndex).trim();
                String value = line.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }
        }

        // Handle body if present. If Content-Length was provided, respect it.
        if (bodySection != null && !bodySection.isEmpty()) {
            if (contentLength > 0) {
                if (bodySection.length() > contentLength) {
                    setBody(bodySection.substring(0, contentLength));
                } else {
                    setBody(bodySection);
                }
            } else {
                setBody(bodySection);
                contentLength = bodySection.length();
            }
        }
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public Map<String, String> getAllHeaders() {
        return headers;
    }

    public HttpMethods getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public QueryString getQueryString() {
        return queryString;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public String getHost() {
        return host;
    }

    public int getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public boolean hasBody() {
        return contentLength > 0;
    }

    private void setBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }
}
