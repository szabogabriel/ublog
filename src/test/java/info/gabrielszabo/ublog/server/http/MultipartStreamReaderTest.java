package info.gabrielszabo.ublog.server.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class MultipartStreamReaderTest {

    private static final String CONTENT_TYPE = "multipart/form-data; boundary=XyZ";

    private static byte[] body(byte[] fileContent) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("--XyZ\r\nContent-Disposition: form-data; name=\"csrfToken\"\r\n\r\ntok\r\n"
                + "--XyZ\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        out.write(fileContent);
        out.write("\r\n--XyZ--\r\n".getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    /** Hands out at most a few bytes per read so delimiters straddle buffer refills. */
    private static InputStream trickle(byte[] data, int maxChunk) {
        ByteArrayInputStream source = new ByteArrayInputStream(data);
        return new InputStream() {
            @Override
            public int read() {
                return source.read();
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                return source.read(buffer, offset, Math.min(length, maxChunk));
            }
        };
    }

    private static byte[][] parse(byte[] body, int maxChunk) throws IOException {
        Map<String, String> fields = new LinkedHashMap<>();
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        MultipartStreamReader.parse(CONTENT_TYPE, trickle(body, maxChunk), new MultipartStreamReader.Sink() {
            @Override
            public void field(String name, String value) {
                fields.put(name, value);
            }

            @Override
            public OutputStream file(String fieldName, String fileName) {
                assertEquals("file", fieldName);
                assertEquals("a.png", fileName);
                return file;
            }
        });
        return new byte[][] { fields.get("csrfToken").getBytes(StandardCharsets.UTF_8), file.toByteArray() };
    }

    @Test
    void largeBinaryFileSurvivesAnyReadChunking() throws IOException {
        byte[] content = new byte[100_000];
        new Random(1).nextBytes(content);
        // Embed something that looks like the start of a delimiter.
        System.arraycopy("\r\n--Xy".getBytes(StandardCharsets.ISO_8859_1), 0, content, 5000, 6);

        for (int chunk : new int[] { 1, 3, 7, 4096, Integer.MAX_VALUE }) {
            byte[][] result = parse(body(content), chunk);
            assertEquals("tok", new String(result[0], StandardCharsets.UTF_8));
            assertArrayEquals(content, result[1]);
        }
    }

    @Test
    void emptyFilePartIsAccepted() throws IOException {
        assertArrayEquals(new byte[0], parse(body(new byte[0]), 5)[1]);
    }

    @Test
    void truncatedBodyIsRejected() {
        byte[] full = new byte[0];
        try {
            full = body(new byte[] { 1, 2, 3 });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        byte[] truncated = java.util.Arrays.copyOf(full, full.length - 12);
        assertThrows(IllegalArgumentException.class, () -> parse(truncated, 4));
    }
}
