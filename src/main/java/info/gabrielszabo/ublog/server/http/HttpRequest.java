package info.gabrielszabo.ublog.server.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpRequest {

    private static final int MAX_HEADER_BYTES = 16 * 1024;
    private static final int MAX_REQUEST_BYTES = 1024 * 1024;
    private static final byte[] HEADER_TERMINATOR = { '\r', '\n', '\r', '\n' };

    private HttpMethods method;
    private String path;
    private QueryString queryString;
    private String httpVersion;
    private String host;
    private int contentLength;
    private String contentType;
    private MimeType mimeType;
    private List<Cookie> cookies = new ArrayList<>();

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
                parseCookies(line.substring(7));
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

    private void parseCookies(String cookieHeader) {
        for (String pair : cookieHeader.split(";")) {
            String trimmedPair = pair.trim();
            if (trimmedPair.isEmpty()) {
                continue;
            }
            try {
                cookies.add(new Cookie(trimmedPair));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed client cookies; other valid cookies remain usable.
            }
        }
    }

    public static HttpRequest readFrom(InputStream input) throws IOException {
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        int matchedTerminatorBytes = 0;
        int current;
        while ((current = input.read()) != -1) {
            headerBytes.write(current);
            if (headerBytes.size() > MAX_HEADER_BYTES) {
                throw new RequestException(431, "Request headers too large");
            }
            if (current == HEADER_TERMINATOR[matchedTerminatorBytes]) {
                matchedTerminatorBytes++;
                if (matchedTerminatorBytes == HEADER_TERMINATOR.length) {
                    break;
                }
            } else {
                matchedTerminatorBytes = current == '\r' ? 1 : 0;
            }
        }
        if (matchedTerminatorBytes != HEADER_TERMINATOR.length) {
            throw new RequestException(400, "Incomplete HTTP headers");
        }

        byte[] header = headerBytes.toByteArray();
        String headerSection = new String(header, StandardCharsets.ISO_8859_1);
        int contentLength = getContentLength(headerSection);
        if (header.length + contentLength > MAX_REQUEST_BYTES) {
            throw new RequestException(413, "Request too large");
        }

        byte[] bodyBytes = new byte[contentLength];
        int bodyOffset = 0;
        while (bodyOffset < bodyBytes.length) {
            int bytesRead = input.read(bodyBytes, bodyOffset, bodyBytes.length - bodyOffset);
            if (bytesRead == -1) {
                throw new RequestException(400, "Incomplete HTTP request body");
            }
            bodyOffset += bytesRead;
        }
        Charset bodyCharset = getBodyCharset(headerSection);
        String body = new String(bodyBytes, bodyCharset);
        return new HttpRequest(headerSection + body);
    }

    private static Charset getBodyCharset(String headerSection) throws RequestException {
        for (String line : headerSection.split("\\r\\n")) {
            if (line.regionMatches(true, 0, "Content-Type:", 0, 13)) {
                String[] parameters = line.substring(13).split(";");
                for (int i = 1; i < parameters.length; i++) {
                    String parameter = parameters[i].trim();
                    if (parameter.regionMatches(true, 0, "charset=", 0, 8)) {
                        String charsetName = parameter.substring(8).trim().replace("\"", "");
                        try {
                            return Charset.forName(charsetName);
                        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                            throw new RequestException(400, "Unsupported request charset");
                        }
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static int getContentLength(String headerSection) throws RequestException {
        String[] lines = headerSection.substring(0, headerSection.length() - 4).split("\\r\\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new RequestException(400, "Missing request line");
        }
        String[] requestLine = lines[0].split(" +");
        if (requestLine.length != 3 || HttpMethods.fromString(requestLine[0]) == null
                || !requestLine[2].startsWith("HTTP/")) {
            throw new RequestException(400, "Malformed request line");
        }

        Integer contentLength = null;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new RequestException(400, "Malformed request header");
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (name.equalsIgnoreCase("Transfer-Encoding")) {
                throw new RequestException(501, "Transfer-Encoding is not supported");
            }
            if (name.equalsIgnoreCase("Content-Length")) {
                try {
                    int parsedLength = Integer.parseInt(value);
                    if (parsedLength < 0 || (contentLength != null && contentLength != parsedLength)) {
                        throw new NumberFormatException();
                    }
                    contentLength = parsedLength;
                } catch (NumberFormatException e) {
                    throw new RequestException(400, "Invalid Content-Length");
                }
            }
        }
        return contentLength == null ? 0 : contentLength;
    }

    public static class RequestException extends IOException {
        private final int statusCode;

        public RequestException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
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

    public List<Cookie> getCookies() {
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
