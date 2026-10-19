package info.gabrielszabo.ublog.server.http;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal multipart/form-data parser; only textual fields are kept, file parts are skipped. */
public final class MultipartForm {

    private static final Pattern BOUNDARY = Pattern.compile("boundary=(?:\"([^\"]+)\"|([^;\\s]+))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_NAME = Pattern.compile("(?:^|;)\\s*name=\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_NAME = Pattern.compile("(?:^|;)\\s*filename\\*?=", Pattern.CASE_INSENSITIVE);

    private final Map<String, String> fields;

    private MultipartForm(Map<String, String> fields) {
        this.fields = fields;
    }

    public static boolean isMultipart(String contentType) {
        return contentType != null && contentType.trim().toLowerCase().startsWith("multipart/form-data");
    }

    public static MultipartForm parse(String contentType, String body) {
        if (!isMultipart(contentType) || body == null) {
            throw new IllegalArgumentException("Not a multipart body");
        }
        Matcher boundaryMatcher = BOUNDARY.matcher(contentType);
        if (!boundaryMatcher.find()) {
            throw new IllegalArgumentException("Missing multipart boundary");
        }
        String boundary = boundaryMatcher.group(1) != null ? boundaryMatcher.group(1) : boundaryMatcher.group(2);
        String delimiter = "\r\n--" + boundary;

        // The leading CRLF lets the first delimiter match like all the others.
        String data = "\r\n" + body;
        Map<String, String> fields = new LinkedHashMap<>();
        int start = data.indexOf(delimiter);
        if (start < 0) {
            throw new IllegalArgumentException("Malformed multipart body");
        }
        boolean closed = false;
        while (!closed) {
            int partStart = start + delimiter.length();
            if (data.startsWith("--", partStart)) {
                break;
            }
            if (!data.startsWith("\r\n", partStart)) {
                throw new IllegalArgumentException("Malformed multipart boundary line");
            }
            partStart += 2;
            int next = data.indexOf(delimiter, partStart);
            if (next < 0) {
                throw new IllegalArgumentException("Unterminated multipart body");
            }
            addPart(fields, data.substring(partStart, next));
            start = next;
        }
        return new MultipartForm(fields);
    }

    private static void addPart(Map<String, String> fields, String part) {
        int headerEnd = part.indexOf("\r\n\r\n");
        String headers;
        String value;
        if (part.startsWith("\r\n")) {
            headers = "";
            value = part.substring(2);
        } else if (headerEnd < 0) {
            throw new IllegalArgumentException("Malformed multipart part");
        } else {
            headers = part.substring(0, headerEnd);
            value = part.substring(headerEnd + 4);
        }

        String disposition = null;
        for (String header : headers.split("\r\n")) {
            if (header.regionMatches(true, 0, "Content-Disposition:", 0, 20)) {
                disposition = header.substring(20);
            }
        }
        if (disposition == null) {
            throw new IllegalArgumentException("Missing Content-Disposition");
        }
        Matcher name = FIELD_NAME.matcher(disposition);
        if (!name.find()) {
            throw new IllegalArgumentException("Missing field name");
        }
        if (FILE_NAME.matcher(disposition).find()) {
            return;
        }
        if (fields.putIfAbsent(name.group(1), value) != null) {
            throw new IllegalArgumentException("Duplicate form field");
        }
    }

    public String get(String name) {
        return fields.get(name);
    }
}
