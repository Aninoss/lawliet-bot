package General.Internet;

public class InternetResponse {
    private String content, cookie;

    public InternetResponse(String content, String cookie) {
        this.content = content;
        this.cookie = cookie;
    }

    public String getContent() {
        return content;
    }

    public String getCookie() {
        return cookie;
    }
}
