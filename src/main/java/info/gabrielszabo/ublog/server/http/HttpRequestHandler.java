package info.gabrielszabo.ublog.server.http;

public interface HttpRequestHandler {

    HttpResponse handleRequest(HttpRequest request);

}
