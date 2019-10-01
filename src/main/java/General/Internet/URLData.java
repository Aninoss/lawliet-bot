package General.Internet;

import java.time.Instant;

public class URLData {
    private String url;
    private Instant blockInstant, createInstant;
    private InternetResponse data;

    public URLData(String url, InternetResponse data) {
        this.url = url;
        this.data = data;
        this.createInstant = Instant.now();
    }

    public String getUrl() {
        return url;
    }

    public InternetResponse getData() {
        return data;
    }

    public void setData(InternetResponse data) {
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
