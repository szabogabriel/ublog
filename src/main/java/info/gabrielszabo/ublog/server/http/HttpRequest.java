package info.gabrielszabo.ublog.server.http;

import java.io.ByteArrayInputStream;
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
import java.util.Locale;
import java.util.Map;

public class HttpRequest {

    private static final int MAX_HEADER_BYTES = 16 * 1024;
    private static final int MAX_REQUEST_BYTES = 1024 * 1024;
    private static final int MAX_IMAGE_UPLOAD_BYTES = 100 * 1024 * 1024;
    private static final String IMAGE_UPLOAD_PATH_PREFIX = "/imagemanage/";
    private static final int MAX_AUTH_REQUEST_BODY_BYTES = 8 * 1024;
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
    private byte[] bodyBytes;
    private InputStream bodyStream;
    private Charset bodyCharset = StandardCharsets.UTF_8;

    public HttpRequest() {
        headers = new LinkedHashMap<>();
    }

    public HttpRequest(String rawRequest) {
        this();
        int separator = rawRequest.indexOf("\r\n\r\n");
        String headerSection = separator < 0 ? rawRequest + "\r\n\r\n"
                : rawRequest.substring(0, separator + 4);
        try {
            parseHeaders(headerSection);
        } catch (RequestException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        if (separator >= 0) {
            body = rawRequest.substring(separator + 4);
            if (contentLength == 0) {
                contentLength = body.getBytes(StandardCharsets.UTF_8).length;
            }
            if (isAuthenticationPath(path) && contentLength > MAX_AUTH_REQUEST_BODY_BYTES) {
                throw new IllegalArgumentException("Authentication request body too large");
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
                throw new RequestException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE, "Request headers too large");
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
            throw new RequestException(HttpStatus.BAD_REQUEST, "Incomplete HTTP headers");
        }

        byte[] header = headerBytes.toByteArray();
        if (header.length > MAX_REQUEST_BYTES) {
            throw new RequestException(HttpStatus.PAYLOAD_TOO_LARGE, "Request too large");
        }
        HttpRequest request = new HttpRequest();
        String headerSection = new String(header, StandardCharsets.ISO_8859_1);
        int contentLength = request.parseHeaders(headerSection);
        if (request.isStreamedUpload()) {
            if (contentLength > MAX_IMAGE_UPLOAD_BYTES) {
                throw new RequestException(HttpStatus.PAYLOAD_TOO_LARGE, "Request too large");
            }
            request.bodyStream = new LimitedInputStream(input, contentLength);
            return request;
        }
        if (contentLength > MAX_REQUEST_BYTES - header.length) {
            throw new RequestException(HttpStatus.PAYLOAD_TOO_LARGE, "Request too large");
        }

        byte[] bodyBytes = new byte[contentLength];
        int bodyOffset = 0;
        while (bodyOffset < bodyBytes.length) {
            int bytesRead = input.read(bodyBytes, bodyOffset, bodyBytes.length - bodyOffset);
            if (bytesRead == -1) {
                throw new RequestException(HttpStatus.BAD_REQUEST, "Incomplete HTTP request body");
            }
            bodyOffset += bytesRead;
        }
        request.bodyCharset = request.getBodyCharset();
        request.bodyBytes = bodyBytes;
        return request;
    }

    // Uploads can be large, so their body is left on the socket for the handler to consume.
    private boolean isStreamedUpload() {
        return method == HttpMethods.POST && path != null
                && path.toLowerCase(Locale.ROOT).startsWith(IMAGE_UPLOAD_PATH_PREFIX);
    }

    private static final class LimitedInputStream extends InputStream {
        private final InputStream source;
        private int remaining;

        LimitedInputStream(InputStream source, int limit) {
            this.source = source;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = source.read();
            if (value >= 0) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int count = source.read(buffer, offset, Math.min(length, remaining));
            if (count > 0) {
                remaining -= count;
            }
            return count;
        }
    }

    private Charset getBodyCharset() throws RequestException {
        if (contentType != null) {
            String[] parameters = contentType.split(";");
            for (int i = 1; i < parameters.length; i++) {
                String parameter = parameters[i].trim();
                if (parameter.regionMatches(true, 0, "charset=", 0, 8)) {
                    String charsetName = parameter.substring(8).trim().replace("\"", "");
                    try {
                        return Charset.forName(charsetName);
                    } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                        throw new RequestException(HttpStatus.BAD_REQUEST, "Unsupported request charset");
                    }
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    private int parseHeaders(String headerSection) throws RequestException {
        String[] lines = headerSection.substring(0, headerSection.length() - 4).split("\\r\\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new RequestException(HttpStatus.BAD_REQUEST, "Missing request line");
        }
        String[] requestLine = lines[0].split(" +");
        if (requestLine.length != 3 || HttpMethods.fromString(requestLine[0]) == null
                || !requestLine[2].startsWith("HTTP/")) {
            throw new RequestException(HttpStatus.BAD_REQUEST, "Malformed request line");
        }
        method = HttpMethods.fromString(requestLine[0]);
        httpVersion = requestLine[2];
        String[] pathParts = requestLine[1].split("\\?", 2);
        path = pathParts[0];
        if (pathParts.length > 1) {
            queryString = new QueryString(pathParts[1]);
        }

        Integer contentLength = null;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new RequestException(HttpStatus.BAD_REQUEST, "Malformed request header");
            }
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            String lowerName = name.toLowerCase(Locale.ROOT);
            if (lowerName.equals("host")) {
                host = value;
            } else if (lowerName.equals("content-type")) {
                contentType = value;
                mimeType = MimeType.fromString(contentType);
            } else if (lowerName.equals("cookie")) {
                parseCookies(value);
            }
            if (name.equalsIgnoreCase("Transfer-Encoding")) {
                throw new RequestException(HttpStatus.NOT_IMPLEMENTED, "Transfer-Encoding is not supported");
            }
            if (name.equalsIgnoreCase("Content-Length")) {
                try {
                    if (value.isEmpty() || !value.chars().allMatch(character -> character >= '0' && character <= '9')) {
                        throw new NumberFormatException();
                    }
                    int parsedLength = Integer.parseInt(value);
                    if (parsedLength < 0 || (contentLength != null && contentLength != parsedLength)) {
                        throw new NumberFormatException();
                    }
                    contentLength = parsedLength;
                } catch (NumberFormatException e) {
                    throw new RequestException(HttpStatus.BAD_REQUEST, "Invalid Content-Length");
                }
            }
            headers.put(name, value);
        }
        this.contentLength = contentLength == null ? 0 : contentLength;
        if (isAuthenticationPath(path) && this.contentLength > MAX_AUTH_REQUEST_BODY_BYTES) {
            throw new RequestException(HttpStatus.PAYLOAD_TOO_LARGE, "Authentication request body too large");
        }
        return this.contentLength;
    }

    private boolean isAuthenticationPath(String requestPath) {
        return requestPath.equals("/login") || requestPath.equals("/loginaction")
                || requestPath.equals("/logout") || requestPath.equals("/passwordchange");
    }

    public static class RequestException extends IOException {
        private final HttpStatus status;

        public RequestException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
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

    public String getBody() {
        if (body == null && bodyBytes != null) {
            body = new String(bodyBytes, bodyCharset);
        }
        return body;
    }

    public byte[] getBodyBytes() {
        if (bodyBytes == null && body != null) {
            bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        }
        return bodyBytes;
    }

    public InputStream getBodyStream() {
        if (bodyStream != null) {
            return bodyStream;
        }
        byte[] bytes = getBodyBytes();
        return new ByteArrayInputStream(bytes == null ? new byte[0] : bytes);
    }
}
