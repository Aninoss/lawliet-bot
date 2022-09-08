package modules.mandaupdates;

public class MangaUpdatesRelease {

    private final String chapter;
    private final String scanlator;

    public MangaUpdatesRelease(String chapter, String scanlator) {
        this.chapter = chapter;
        this.scanlator = scanlator;
    }

    public String getChapter() {
        return chapter;
    }

    public String getScanlator() {
        return scanlator;
    }

    public String getId() {
        return chapter + ":" + scanlator;
    }

}
