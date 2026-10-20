package info.gabrielszabo.ublog.server.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Buffered view of a small multipart/form-data body; only textual fields are kept, file parts are skipped. */
public final class MultipartForm {

    private final Map<String, String> fields;

    private MultipartForm(Map<String, String> fields) {
        this.fields = fields;
    }

    public static boolean isMultipart(String contentType) {
        return MultipartStreamReader.isMultipart(contentType);
    }

    public static MultipartForm parse(String contentType, byte[] body) {
        if (body == null) {
            throw new IllegalArgumentException("Not a multipart body");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        try {
            MultipartStreamReader.parse(contentType, new ByteArrayInputStream(body),
                    new MultipartStreamReader.Sink() {
                        @Override
                        public void field(String name, String value) {
                            if (fields.putIfAbsent(name, value) != null) {
                                throw new IllegalArgumentException("Duplicate form field");
                            }
                        }

                        @Override
                        public OutputStream file(String fieldName, String fileName) {
                            return OutputStream.nullOutputStream();
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new MultipartForm(fields);
    }

    public String get(String name) {
        return fields.get(name);
    }
}
