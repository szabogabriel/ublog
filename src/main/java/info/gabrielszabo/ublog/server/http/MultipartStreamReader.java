package info.gabrielszabo.ublog.server.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Incremental multipart/form-data parser; file parts are written straight to a caller-supplied stream. */
public final class MultipartStreamReader {

    public interface Sink {
        void field(String name, String value) throws IOException;

        /** Returns the stream receiving the file content; it is closed once the part ends. */
        OutputStream file(String fieldName, String fileName) throws IOException;
    }

    private static final Pattern BOUNDARY = Pattern.compile("boundary=(?:\"([^\"]+)\"|([^;\\s]+))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FIELD_NAME = Pattern.compile("(?:^|;)\\s*name=\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_NAME = Pattern.compile("(?:^|;)\\s*filename=\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE);
    private static final byte[] CRLF = { '\r', '\n' };
    private static final byte[] DASHES = { '-', '-' };
    private static final byte[] HEADER_END = { '\r', '\n', '\r', '\n' };
    private static final int BUFFER_SIZE = 16 * 1024;
    private static final int MAX_HEADER_BYTES = 8 * 1024;
    private static final int MAX_FIELD_BYTES = 256 * 1024;

    private final InputStream input;
    private final byte[] delimiter;
    private final byte[] buffer = new byte[BUFFER_SIZE];
    private int position;
    private int limit;

    private MultipartStreamReader(InputStream input, byte[] delimiter) {
        this.input = input;
        this.delimiter = delimiter;
    }

    public static boolean isMultipart(String contentType) {
        return contentType != null && contentType.trim().toLowerCase().startsWith("multipart/form-data");
    }

    /** Malformed input raises IllegalArgumentException; sink and stream failures pass through. */
    public static void parse(String contentType, InputStream input, Sink sink) throws IOException {
        if (!isMultipart(contentType) || input == null) {
            throw new IllegalArgumentException("Not a multipart body");
        }
        Matcher boundaryMatcher = BOUNDARY.matcher(contentType);
        if (!boundaryMatcher.find()) {
            throw new IllegalArgumentException("Missing multipart boundary");
        }
        String boundary = boundaryMatcher.group(1) != null ? boundaryMatcher.group(1) : boundaryMatcher.group(2);
        byte[] delimiter = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        new MultipartStreamReader(input, delimiter).readParts(sink);
    }

    private void readParts(Sink sink) throws IOException {
        // A virtual CRLF lets the first delimiter match like all the others; the preamble is discarded.
        buffer[0] = '\r';
        buffer[1] = '\n';
        limit = 2;
        copyUntil(delimiter, OutputStream.nullOutputStream(), Integer.MAX_VALUE);

        while (true) {
            ensure(2);
            if (startsWith(DASHES)) {
                return;
            }
            if (!startsWith(CRLF)) {
                throw new IllegalArgumentException("Malformed multipart boundary line");
            }
            position += 2;
            readPart(sink);
        }
    }

    private void readPart(Sink sink) throws IOException {
        String headers = "";
        ensure(2);
        if (startsWith(CRLF)) {
            position += 2;
        } else {
            ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
            copyUntil(HEADER_END, headerBytes, MAX_HEADER_BYTES);
            headers = headerBytes.toString(StandardCharsets.UTF_8);
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
        Matcher fileName = FILE_NAME.matcher(disposition);
        if (fileName.find()) {
            try (OutputStream out = sink.file(name.group(1), fileName.group(1))) {
                copyUntil(delimiter, out, Integer.MAX_VALUE);
            }
        } else {
            ByteArrayOutputStream value = new ByteArrayOutputStream();
            copyUntil(delimiter, value, MAX_FIELD_BYTES);
            sink.field(name.group(1), value.toString(StandardCharsets.UTF_8));
        }
    }

    /** Copies bytes up to the next occurrence of the pattern and consumes the pattern. */
    private void copyUntil(byte[] pattern, OutputStream out, int maxBytes) throws IOException {
        long copied = 0;
        while (true) {
            int index = indexOf(pattern);
            if (index >= 0) {
                out.write(buffer, position, index - position);
                position = index + pattern.length;
                return;
            }
            // Keep a possible partial match at the end of the buffer for the next read.
            int safe = limit - position - (pattern.length - 1);
            if (safe > 0) {
                copied += safe;
                if (copied > maxBytes) {
                    throw new IllegalArgumentException("Multipart part too large");
                }
                out.write(buffer, position, safe);
                position += safe;
            }
            if (!fill()) {
                throw new IllegalArgumentException("Unterminated multipart body");
            }
        }
    }

    private void ensure(int count) throws IOException {
        while (limit - position < count) {
            if (!fill()) {
                throw new IllegalArgumentException("Unterminated multipart body");
            }
        }
    }

    private boolean fill() throws IOException {
        if (position > 0) {
            System.arraycopy(buffer, position, buffer, 0, limit - position);
            limit -= position;
            position = 0;
        }
        int read = input.read(buffer, limit, buffer.length - limit);
        if (read < 0) {
            return false;
        }
        limit += read;
        return true;
    }

    private boolean startsWith(byte[] pattern) {
        return matchesAt(position, pattern);
    }

    private boolean matchesAt(int offset, byte[] pattern) {
        if (offset + pattern.length > limit) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (buffer[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private int indexOf(byte[] pattern) {
        for (int i = position; i <= limit - pattern.length; i++) {
            if (matchesAt(i, pattern)) {
                return i;
            }
        }
        return -1;
    }
}
