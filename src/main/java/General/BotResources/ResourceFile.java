package General.BotResources;

public class ResourceFile {
    private String fileName, URL;

    public ResourceFile(String fileName, String URL) {
        this.fileName = fileName;
        this.URL = URL;
    }

    public String getFileName() {
        return fileName;
    }

    public String getURL() {
        return URL;
    }
}
