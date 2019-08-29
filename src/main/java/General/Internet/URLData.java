package General.Internet;

import java.time.Instant;

public class URLData {
    private String url, data;
    private Instant blockInstant, createInstant;

    public URLData(String url, String data) {
        this.url = url;
        this.data = data;
        this.createInstant = Instant.now();
    }

    public String getUrl() {
        return url;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Instant getBlockInstant() {
        return blockInstant;
    }

    public void setBlockInstant(Instant blockInstant) {
        this.blockInstant = blockInstant;
    }

    public Instant getCreateInstant() {
        return createInstant;
    }
}
