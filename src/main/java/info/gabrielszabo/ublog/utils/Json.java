package info.gabrielszabo.ublog.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal JSON reader/writer: objects, arrays, strings, numbers, booleans and null. */
public final class Json {

    private static final int MAX_DEPTH = 64;

    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue(0);
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("Unexpected trailing content");
        }
        return value;
    }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private static void write(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof Boolean || value instanceof Number) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(String.valueOf(entry.getKey()), sb);
                sb.append(':');
                write(entry.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof Iterable<?> items) {
            sb.append('[');
            boolean first = true;
            for (Object item : items) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(item, sb);
            }
            sb.append(']');
        } else {
            writeString(value.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private Object readValue(int depth) {
        if (depth > MAX_DEPTH) {
            throw error("Nesting too deep");
        }
        if (pos >= text.length()) {
            throw error("Unexpected end of input");
        }
        char c = text.charAt(pos);
        if (c == '{') {
            return readObject(depth);
        }
        if (c == '[') {
            return readArray(depth);
        }
        if (c == '"') {
            return readString();
        }
        if (text.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        if (text.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        return readNumber();
    }

    private Map<String, Object> readObject(int depth) {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Expected string key");
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, readValue(depth + 1));
            skipWhitespace();
            if (peek() == ',') {
                pos++;
            } else {
                expect('}');
                return map;
            }
        }
    }

    private List<Object> readArray(int depth) {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(readValue(depth + 1));
            skipWhitespace();
            if (peek() == ',') {
                pos++;
            } else {
                expect(']');
                return list;
            }
        }
    }

    private String readString() {
        StringBuilder sb = new StringBuilder();
        pos++;
        while (true) {
            if (pos >= text.length()) {
                throw error("Unterminated string");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= text.length()) {
                throw error("Unterminated escape");
            }
            char escaped = text.charAt(pos++);
            switch (escaped) {
                case '"', '\\', '/' -> sb.append(escaped);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw error("Invalid unicode escape");
                    }
                    try {
                        sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    } catch (NumberFormatException e) {
                        throw error("Invalid unicode escape");
                    }
                    pos += 4;
                }
                default -> throw error("Invalid escape");
            }
        }
    }

    private Number readNumber() {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        String number = text.substring(start, pos);
        try {
            if (number.isEmpty()) {
                throw error("Unexpected character");
            }
            if (number.contains(".") || number.contains("e") || number.contains("E")) {
                return Double.parseDouble(number);
            }
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            throw error("Invalid number");
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw error("Unexpected end of input");
        }
        return text.charAt(pos);
    }

    private void expect(char c) {
        if (peek() != c) {
            throw error("Expected '" + c + "'");
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + pos);
    }
}
