package info.gabrielszabo.ublog.server.http;

import java.nio.charset.StandardCharsets;

public class HttpResponse {

    private String httpVersion = "1.1";
    private HttpStatus status;
    private String body;
    private String contentType = "text/plain; charset=UTF-8";

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

    public String toHttpResponseMessage() {
        StringBuilder response = new StringBuilder();
        String bodyStr = (body == null) ? "" : body;
        int contentLength = bodyStr.getBytes(StandardCharsets.UTF_8).length;
        response.append(httpVersion.startsWith("HTTP/") ? httpVersion : "HTTP/" + httpVersion).append(" ")
                .append(status.toStatusLine()).append("\r\n")
                .append("Content-Type: ").append(contentType).append("\r\n")
                .append("Content-Length: ").append(contentLength).append("\r\n")
                .append("\r\n")
                .append(bodyStr);
        return response.toString();
    }

}
