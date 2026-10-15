package info.gabrielszabo.ublog.server.http;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpResponse {

    private String httpVersion = "1.1";
    private HttpStatus status;
    private String body;
    private String contentType = "text/plain; charset=UTF-8";
    private final List<Cookie> cookies = new ArrayList<>();
    private final Map<String, String> headers = new LinkedHashMap<>();

    public HttpResponse() {
        // Constructor implementation
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public void setStatus(HttpStatus status) {
        this.status = status;
    }

    public void setStatusCode(int statusCode) {
        this.status = HttpStatus.fromCode(statusCode);
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void addCookie(Cookie cookie) {
        this.cookies.add(cookie);
    }

    public void removeCookie(Cookie cookie) {
        this.cookies.remove(cookie);
    }

    public void addHeader(String name, String value) {
        if (name == null || name.isEmpty() || value == null) {
            throw new IllegalArgumentException("Invalid HTTP response header");
        }
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (!(character >= '0' && character <= '9' || character >= 'A' && character <= 'Z'
                    || character >= 'a' && character <= 'z' || "!#$%&'*+-.^_`|~".indexOf(character) >= 0)) {
                throw new IllegalArgumentException("Invalid HTTP response header name");
            }
        }
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Invalid HTTP response header value");
        }
        headers.put(name, value);
    }

    public String toHttpResponseMessage() {
        StringBuilder response = new StringBuilder();
        String bodyStr = (body == null) ? "" : body;
        int contentLength = bodyStr.getBytes(StandardCharsets.UTF_8).length;
        response.append(httpVersion.startsWith("HTTP/") ? httpVersion : "HTTP/" + httpVersion).append(" ")
                .append(status.toStatusLine()).append("\r\n")
                .append("Content-Type: ").append(contentType).append("\r\n")
                .append("Content-Length: ").append(contentLength).append("\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            response.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
        if (!cookies.isEmpty()) {
            for (Cookie cookie : cookies) {
                response.append("Set-Cookie: ").append(cookie.toString()).append("\r\n");
            }
        }
        response.append("\r\n")
                .append(bodyStr);
        return response.toString();
    }

}
