package info.gabrielszabo.ublog.server.http;

public enum HttpMethods {
    GET,
    POST,
    PUT,
    DELETE,
    HEAD,
    OPTIONS,
    PATCH,
    TRACE,
    CONNECT,
    ;

    public static HttpMethods fromString(String method) {
        for (HttpMethods httpMethod : HttpMethods.values()) {
            if (httpMethod.name().equalsIgnoreCase(method)) {
                return httpMethod;
            }
        }
        return null;
    }
}
