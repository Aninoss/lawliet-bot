package modules.pixiv;

public class PixivChoice {

    private String tag;
    private String translatedTag;

    public String getTag() {
        return tag;
    }

    public PixivChoice setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public String getTranslatedTag() {
        return translatedTag;
    }

    public PixivChoice setTranslatedTag(String translatedTag) {
        this.translatedTag = translatedTag;
        return this;
    }
}
