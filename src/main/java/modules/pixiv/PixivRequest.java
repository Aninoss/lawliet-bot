package modules.pixiv;

import java.util.List;

public class PixivRequest {

    private Long guildId;
    private String word;
    private boolean nsfwAllowed;
    private List<String> filters;
    private List<String> strictFilters;

    public Long getGuildId() {
        return guildId;
    }

    public PixivRequest setGuildId(Long guildId) {
        this.guildId = guildId;
        return this;
    }

    public String getWord() {
        return word;
    }

    public PixivRequest setWord(String word) {
        this.word = word;
        return this;
    }

    public boolean isNsfwAllowed() {
        return nsfwAllowed;
    }

    public PixivRequest setNsfwAllowed(boolean nsfwAllowed) {
        this.nsfwAllowed = nsfwAllowed;
        return this;
    }

    public List<String> getFilters() {
        return filters;
    }

    public PixivRequest setFilters(List<String> filters) {
        this.filters = filters;
        return this;
    }

    public List<String> getStrictFilters() {
        return strictFilters;
    }

    public PixivRequest setStrictFilters(List<String> strictFilters) {
        this.strictFilters = strictFilters;
        return this;
    }
}
