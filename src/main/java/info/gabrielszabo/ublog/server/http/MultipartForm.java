package info.gabrielszabo.ublog.server.http;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal multipart/form-data parser working on raw bytes, so uploaded files stay intact. */
public final class MultipartForm {

    private static final Pattern BOUNDARY = Pattern.compile("boundary=(?:\"([^\"]+)\"|([^;\\s]+))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_NAME = Pattern.compile("(?:^|;)\\s*name=\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_NAME = Pattern.compile("(?:^|;)\\s*filename=\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE);
    private static final byte[] CRLF = { '\r', '\n' };
    private static final byte[] DASHES = { '-', '-' };
    private static final byte[] HEADER_END = { '\r', '\n', '\r', '\n' };

    public record UploadedFile(String fileName, byte[] content) {
    }

    private final Map<String, String> fields;
    private final Map<String, UploadedFile> files;

    private MultipartForm(Map<String, String> fields, Map<String, UploadedFile> files) {
        this.fields = fields;
        this.files = files;
    }

    public static boolean isMultipart(String contentType) {
        return contentType != null && contentType.trim().toLowerCase().startsWith("multipart/form-data");
    }

    public static MultipartForm parse(String contentType, byte[] body) {
        if (!isMultipart(contentType) || body == null) {
            throw new IllegalArgumentException("Not a multipart body");
        }
        Matcher boundaryMatcher = BOUNDARY.matcher(contentType);
        if (!boundaryMatcher.find()) {
            throw new IllegalArgumentException("Missing multipart boundary");
        }
        String boundary = boundaryMatcher.group(1) != null ? boundaryMatcher.group(1) : boundaryMatcher.group(2);
        byte[] firstDelimiter = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] delimiter = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

        Map<String, String> fields = new LinkedHashMap<>();
        Map<String, UploadedFile> files = new LinkedHashMap<>();

        // The first delimiter is not preceded by CRLF when the body starts with it.
        int delimiterEnd;
        if (startsWith(body, firstDelimiter, 0)) {
            delimiterEnd = firstDelimiter.length;
        } else {
            int start = indexOf(body, delimiter, 0);
            if (start < 0) {
                throw new IllegalArgumentException("Malformed multipart body");
            }
            delimiterEnd = start + delimiter.length;
        }
        while (!startsWith(body, DASHES, delimiterEnd)) {
            if (!startsWith(body, CRLF, delimiterEnd)) {
                throw new IllegalArgumentException("Malformed multipart boundary line");
            }
            int partStart = delimiterEnd + 2;
            int next = indexOf(body, delimiter, partStart);
            if (next < 0) {
                throw new IllegalArgumentException("Unterminated multipart body");
            }
            addPart(fields, files, body, partStart, next);
            delimiterEnd = next + delimiter.length;
        }
        return new MultipartForm(fields, files);
    }

    private static void addPart(Map<String, String> fields, Map<String, UploadedFile> files, byte[] body,
            int start, int end) {
        String headers;
        int valueStart;
        if (startsWith(body, CRLF, start)) {
            headers = "";
            valueStart = start + 2;
        } else {
            int headerEnd = indexOf(body, HEADER_END, start);
            if (headerEnd < 0 || headerEnd >= end) {
                throw new IllegalArgumentException("Malformed multipart part");
            }
            headers = new String(body, start, headerEnd - start, StandardCharsets.UTF_8);
            valueStart = headerEnd + 4;
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
        String fieldName = name.group(1);
        Matcher fileName = FILE_NAME.matcher(disposition);
        if (fileName.find()) {
            UploadedFile file = new UploadedFile(fileName.group(1), Arrays.copyOfRange(body, valueStart, end));
            if (files.putIfAbsent(fieldName, file) != null) {
                throw new IllegalArgumentException("Duplicate form field");
            }
            return;
        }
        String value = new String(body, valueStart, end - valueStart, StandardCharsets.UTF_8);
        if (fields.putIfAbsent(fieldName, value) != null) {
            throw new IllegalArgumentException("Duplicate form field");
        }
    }

    private static boolean startsWith(byte[] data, byte[] pattern, int offset) {
        if (offset < 0 || offset + pattern.length > data.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        for (int i = from; i <= data.length - pattern.length; i++) {
            if (startsWith(data, pattern, i)) {
                return i;
            }
        }
        return -1;
    }

    public String get(String name) {
        return fields.get(name);
    }

    public UploadedFile getFile(String name) {
        return files.get(name);
    }
}
