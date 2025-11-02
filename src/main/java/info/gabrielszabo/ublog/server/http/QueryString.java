package info.gabrielszabo.ublog.server.http;

import java.util.HashMap;
import java.util.Map;

public class QueryString {

    private Map<String, String> parameters = new HashMap<>();

    public QueryString(String queryString) {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";
            parameters.put(key, value);
        }
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }

    public Map<String, String> getAllParameters() {
        return parameters;
    }

}
