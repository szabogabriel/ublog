package info.gabrielszabo.ublog.server.http;

public class Cookie {

    private final String name;
    private final String value;
    private final long maxAge;

    public Cookie(String cookieString) {
        String[] parts = cookieString.split("=", 2);
        this.name = parts[0];
        this.value = parts.length > 1 ? parts[1] : "";
        this.maxAge = -1;
    }

    public Cookie(String name, String value) {
        this.name = name;
        this.value = value;
        this.maxAge = -1;
    }

    public Cookie(String name, String value, long maxAge) {
        this.name = name;
        this.value = value;
        this.maxAge = maxAge;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public long getMaxAge() {
        return maxAge;
    }

    @Override
    public String toString() {
        return name + "=" + value + (maxAge >= 0 ? "; Max-Age=" + maxAge : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Cookie)) {
            return false;
        }
        Cookie other = (Cookie) obj;
        return name == null ? other.name == null : name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name == null ? 0 : name.hashCode();
    }
}
