package info.gabrielszabo.ublog.server.http;

public class Cookie {

    private final String name;
    private final String value;
    private final long maxAge;
    private String path;
    private String sameSite;
    private boolean httpOnly;
    private boolean secure;

    public Cookie(String cookieString) {
        String[] parts = cookieString.trim().split("=", 2);
        this.name = validateName(parts[0].trim());
        this.value = validateValue(parts.length > 1 ? parts[1].trim() : "");
        this.maxAge = -1;
    }

    public Cookie(String name, String value) {
        this.name = validateName(name);
        this.value = validateValue(value);
        this.maxAge = -1;
    }

    public Cookie(String name, String value, long maxAge) {
        this.name = validateName(name);
        this.value = validateValue(value);
        this.maxAge = maxAge;
    }

    public Cookie setPath(String path) {
        if (path == null || !path.startsWith("/") || containsControl(path) || path.indexOf(';') >= 0) {
            throw new IllegalArgumentException("Invalid cookie path");
        }
        this.path = path;
        return this;
    }

    public Cookie setSameSite(String sameSite) {
        if (!"Lax".equals(sameSite) && !"Strict".equals(sameSite) && !"None".equals(sameSite)) {
            throw new IllegalArgumentException("Invalid SameSite value");
        }
        this.sameSite = sameSite;
        return this;
    }

    public Cookie setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public Cookie setSecure(boolean secure) {
        this.secure = secure;
        return this;
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
        StringBuilder cookie = new StringBuilder(name).append('=').append(value);
        if (maxAge >= 0) {
            cookie.append("; Max-Age=").append(maxAge);
        }
        if (path != null) {
            cookie.append("; Path=").append(path);
        }
        if (httpOnly) {
            cookie.append("; HttpOnly");
        }
        if (secure) {
            cookie.append("; Secure");
        }
        if (sameSite != null) {
            cookie.append("; SameSite=").append(sameSite);
        }
        return cookie.toString();
    }

    private static String validateName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid cookie name");
        }
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (!isTokenCharacter(character)) {
                throw new IllegalArgumentException("Invalid cookie name");
            }
        }
        return name;
    }

    private static boolean isTokenCharacter(char character) {
        return character >= '0' && character <= '9' || character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z' || "!#$%&'*+-.^_`|~".indexOf(character) >= 0;
    }

    private static String validateValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Invalid cookie value");
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!(character == 0x21 || character >= 0x23 && character <= 0x2B
                    || character >= 0x2D && character <= 0x3A || character >= 0x3C && character <= 0x5B
                    || character >= 0x5D && character <= 0x7E)) {
                throw new IllegalArgumentException("Invalid cookie value");
            }
        }
        return value;
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < 0x20 || value.charAt(i) == 0x7f) {
                return true;
            }
        }
        return false;
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
