package info.gabrielszabo.ublog.server.http;

public enum MimeType {
    TEXT_HTML("text/html"),
    TEXT_PLAIN("text/plain"),
    APPLICATION_JSON("application/json"),
    APPLICATION_XML("application/xml"),
    IMAGE_PNG("image/png"),
    IMAGE_JPEG("image/jpeg"),
    MULTIPART_FORM_DATA("multipart/form-data"),
    IMAGE_GIF("image/gif"),
    IMAGE_BMP("image/bmp"),
    IMAGE_TIFF("image/tiff"),
    IMAGE_WEBP("image/webp"),
    IMAGE_SVG_XML("image/svg+xml"),
    IMAGE_AVIF("image/avif"),
    APPLICATION_PDF("application/pdf"),
    APPLICATION_X_YAML("application/x-yaml"),
    TEXT_YAML("text/yaml"),
    APPLICATION_BINARY("application/octet-stream"),
    ;

    private String value;

    MimeType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MimeType fromString(String mimeType) {
        for (MimeType type : MimeType.values()) {
            if (type.getValue().equalsIgnoreCase(mimeType)) {
                return type;
            }
        }
        return null;
    }
}
