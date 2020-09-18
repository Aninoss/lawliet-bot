package core.internet;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpResponse {

    private String content;
    private int code;
    private Map<String, List<String>> headerFields;

    public HttpResponse(String content, Map<String, List<String>> headerFields, int code) {
        this.content = content;
        this.headerFields = headerFields;
        this.code = code;
    }

    public HttpResponse(int code) {
        this.code = code;
    }

    public Optional<String> getContent() {
        return Optional.ofNullable(content);
    }

    public Optional<Map<String, List<String>>> getHeaderFields() {
        return Optional.ofNullable(headerFields);
    }

    public Optional<List<String>> getCookies() {
        if (headerFields == null) return Optional.empty();
        return Optional.ofNullable(headerFields.get("Set-Cookie"));
    }

    public int getCode() {
        return code;
    }
}
