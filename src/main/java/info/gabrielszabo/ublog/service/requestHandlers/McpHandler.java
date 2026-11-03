package info.gabrielszabo.ublog.service.requestHandlers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import info.gabrielszabo.ublog.domain.SessionContext;
import info.gabrielszabo.ublog.domain.User;
import info.gabrielszabo.ublog.server.http.HttpMethods;
import info.gabrielszabo.ublog.server.http.HttpRequest;
import info.gabrielszabo.ublog.server.http.HttpResponse;
import info.gabrielszabo.ublog.server.http.HttpStatus;
import info.gabrielszabo.ublog.service.ContentService;
import info.gabrielszabo.ublog.service.SessionService;
import info.gabrielszabo.ublog.service.UserService;
import info.gabrielszabo.ublog.service.blog.BlogContext;
import info.gabrielszabo.ublog.service.blog.BlogContextFactory;
import info.gabrielszabo.ublog.service.template.TemplateService;
import info.gabrielszabo.ublog.utils.CryptoUtils;
import info.gabrielszabo.ublog.utils.Json;

/**
 * MCP server (Streamable HTTP transport, JSON-RPC over POST) rooted at /mcp.
 * Authenticated with the per-user token as a bearer token.
 */
public class McpHandler extends AbstractRequestHandler {

    private static final String ROOT_PATH = "/mcp";
    private static final String PROTOCOL_VERSION = "2025-06-18";
    private static final String INCLUDE_UNPUBLISHED = "includeUnpublished";

    private final ContentService contentService;

    public McpHandler(BlogContextFactory contextFactory, SessionService sessionService,
            TemplateService templateService, ContentService contentService, UserService userService) {
        super(contextFactory, sessionService, templateService, userService);
        this.contentService = contentService;
    }

    @Override
    public boolean matches(String path) {
        return path.equals(ROOT_PATH) || path.equals(ROOT_PATH + "/");
    }

    // MCP clients authenticate with a token, not a session cookie.
    @Override
    public HttpResponse handleRequest(HttpRequest request) {
        if (request.getMethod() != HttpMethods.POST) {
            HttpResponse response = plain(HttpStatus.METHOD_NOT_ALLOWED, "MCP requires POST");
            response.addHeader("Allow", "POST");
            return response;
        }
        Optional<User> user = authenticate(request);
        if (user.isEmpty()) {
            HttpResponse response = plain(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
            response.addHeader("WWW-Authenticate", "Bearer");
            return response;
        }

        Object parsed;
        try {
            parsed = Json.parse(request.getBody() == null ? "" : request.getBody());
        } catch (IllegalArgumentException e) {
            return json(errorResponse(null, -32700, "Parse error"));
        }
        if (!(parsed instanceof Map<?, ?> message)) {
            return json(errorResponse(null, -32600, "Invalid request"));
        }

        Object id = message.get("id");
        Object method = message.get("method");
        if (!(method instanceof String methodName)) {
            // A response or malformed message; nothing to answer.
            return id == null ? json(errorResponse(null, -32600, "Invalid request")) : accepted();
        }
        if (id == null) {
            return accepted();
        }
        Map<?, ?> params = message.get("params") instanceof Map<?, ?> p ? p : Map.of();

        switch (methodName) {
            case "initialize":
                return json(result(id, initializeResult()));
            case "ping":
                return json(result(id, new LinkedHashMap<>()));
            case "tools/list":
                return json(result(id, Map.of("tools", toolDefinitions())));
            case "tools/call":
                return json(callTool(id, params, user.get(), request));
            default:
                return json(errorResponse(id, -32601, "Method not found"));
        }
    }

    @Override
    protected HttpResponse handleRequest(BlogContext context, SessionContext session, HttpRequest request) {
        return handleRequest(request);
    }

    private Optional<User> authenticate(HttpRequest request) {
        String authorization = null;
        for (Map.Entry<String, String> header : request.getAllHeaders().entrySet()) {
            if (header.getKey().equalsIgnoreCase("Authorization")) {
                authorization = header.getValue();
            }
        }
        if (authorization == null || authorization.length() <= 7
                || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return Optional.empty();
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        return userService.getUserByToken(CryptoUtils.INSTANCE.hash(token));
    }

    private Map<String, Object> initializeResult() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
        result.put("serverInfo", Map.of("name", "ublog", "version", "1.0"));
        return result;
    }

    private List<Object> toolDefinitions() {
        Map<String, Object> category = prop("string",
                "Category name. Omit or leave empty for entries without a category.");
        Map<String, Object> name = prop("string", "Entry name.");
        Map<String, Object> unpublished = prop("boolean",
                "Include unpublished (unlisted) entries. Defaults to the '" + INCLUDE_UNPUBLISHED
                        + "' query parameter of the request, otherwise false.");

        List<Object> tools = new ArrayList<>();
        tools.add(tool("list_categories", "List the blog categories.", Map.of(), List.of()));
        tools.add(tool("list_entries",
                "List entries. Without 'category' all entries are returned, including those without a category; "
                        + "an empty 'category' returns only the entries without a category.",
                Map.of("category", category, INCLUDE_UNPUBLISHED, unpublished), List.of()));
        tools.add(tool("get_entry", "Get the markdown content of an entry.",
                Map.of("category", category, "name", name, INCLUDE_UNPUBLISHED, unpublished), List.of("name")));
        Map<String, Object> content = prop("string", "Markdown content of the entry.");
        tools.add(tool("create_entry",
                "Create a new entry in unlisted (unpublished) mode. Requires an editor user.",
                Map.of("category", category, "name", name, "content", content), List.of("name", "content")));
        tools.add(tool("update_entry",
                "Update the content of an existing unlisted (unpublished) entry. Requires an editor user. "
                        + "Published entries cannot be changed.",
                Map.of("category", category, "name", name, "content", content), List.of("name", "content")));
        return tools;
    }

    private Map<String, Object> callTool(Object id, Map<?, ?> params, User user, HttpRequest request) {
        if (!(params.get("name") instanceof String toolName)) {
            return errorResponse(id, -32602, "Missing tool name");
        }
        Map<?, ?> args = params.get("arguments") instanceof Map<?, ?> a ? a : Map.of();
        try {
            boolean unpublished = args.get(INCLUDE_UNPUBLISHED) instanceof Boolean b ? b : queryFlag(request);
            switch (toolName) {
                case "list_categories":
                    return result(id, toolResult(Json.write(Map.of("categories", contentService.listTopics())), false));
                case "list_entries":
                    return result(id, listEntries(args, unpublished));
                case "get_entry":
                    return result(id, getEntry(args, unpublished));
                case "create_entry":
                    return result(id, createEntry(args, user));
                case "update_entry":
                    return result(id, updateEntry(args, user));
                default:
                    return errorResponse(id, -32602, "Unknown tool: " + toolName);
            }
        } catch (RuntimeException e) {
            return result(id, toolResult("Tool execution failed", true));
        }
    }

    private Map<String, Object> listEntries(Map<?, ?> args, boolean unpublished) {
        List<Object> entries = new ArrayList<>();
        if (args.get("category") instanceof String category) {
            if (!ContentService.isRoot(category) && !contentService.topicExists(category)) {
                return toolResult("Unknown category", true);
            }
            addEntries(entries, category, unpublished);
        } else {
            addEntries(entries, "", unpublished);
            for (String topic : contentService.listTopics()) {
                addEntries(entries, topic, unpublished);
            }
        }
        return toolResult(Json.write(Map.of("entries", entries)), false);
    }

    private void addEntries(List<Object> target, String topic, boolean unpublished) {
        for (String entry : contentService.listEntries(topic, unpublished)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("category", ContentService.isRoot(topic) ? null : topic);
            item.put("name", entry);
            item.put("published", !contentService.isEntryUnlisted(topic, entry));
            target.add(item);
        }
    }

    private Map<String, Object> getEntry(Map<?, ?> args, boolean unpublished) {
        if (!(args.get("name") instanceof String name)) {
            return toolResult("Missing 'name'", true);
        }
        String content = contentService.getEntryContent(category(args), name, unpublished);
        if (content == null) {
            return toolResult("Entry not found", true);
        }
        return toolResult(content, false);
    }

    private Map<String, Object> createEntry(Map<?, ?> args, User user) {
        if (!Boolean.TRUE.equals(user.getEditor())) {
            return toolResult("User is not an editor", true);
        }
        if (!(args.get("name") instanceof String name) || !(args.get("content") instanceof String content)) {
            return toolResult("Missing 'name' or 'content'", true);
        }
        String normalized = name.trim().replaceAll(" +", "_");
        if (!contentService.createEntry(category(args), name, content, null, true)) {
            return toolResult("The entry could not be created (invalid name or category, or it already exists)",
                    true);
        }
        return toolResult("Created unlisted entry '" + normalized + "'", false);
    }

    private Map<String, Object> updateEntry(Map<?, ?> args, User user) {
        if (!Boolean.TRUE.equals(user.getEditor())) {
            return toolResult("User is not an editor", true);
        }
        if (!(args.get("name") instanceof String name) || !(args.get("content") instanceof String content)) {
            return toolResult("Missing 'name' or 'content'", true);
        }
        String category = category(args);
        if (contentService.getEntryContent(category, name, true) == null) {
            return toolResult("Entry not found", true);
        }
        if (!contentService.isEntryUnlisted(category, name)) {
            return toolResult("Only unlisted entries can be updated", true);
        }
        if (!contentService.updateEntry(category, name, content, true)) {
            return toolResult("The entry could not be updated", true);
        }
        return toolResult("Updated unlisted entry '" + name + "'", false);
    }

    private static String category(Map<?, ?> args) {
        return args.get("category") instanceof String c ? c : "";
    }

    private static boolean queryFlag(HttpRequest request) {
        return request.getQueryString() != null
                && "true".equalsIgnoreCase(request.getQueryString().getParameter(INCLUDE_UNPUBLISHED));
    }

    private static Map<String, Object> prop(String type, String description) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("description", description);
        return map;
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> properties,
            List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", schema);
        return tool;
    }

    private static Map<String, Object> toolResult(String text, boolean error) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of(Map.of("type", "text", "text", text)));
        result.put("isError", error);
        return result;
    }

    private static Map<String, Object> result(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private static Map<String, Object> errorResponse(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of("code", code, "message", message));
        return response;
    }

    private static HttpResponse json(Object body) {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.OK);
        response.setContentType("application/json; charset=UTF-8");
        response.setBody(Json.write(body));
        return response;
    }

    private static HttpResponse accepted() {
        HttpResponse response = new HttpResponse();
        response.setStatus(HttpStatus.ACCEPTED);
        response.setBody("");
        return response;
    }

    private static HttpResponse plain(HttpStatus status, String message) {
        HttpResponse response = new HttpResponse();
        response.setStatus(status);
        response.setContentType("text/plain; charset=UTF-8");
        response.setBody(message);
        return response;
    }
}
