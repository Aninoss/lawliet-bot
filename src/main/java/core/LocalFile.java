package core;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class LocalFile extends File {

    public static final String CDN_ROOT_URL = "https://lawlietbot.xyz/cdn/";

    private String filename = null;

    public LocalFile(Directory directory, @NotNull String filename) {
        super(System.getenv("ROOT_DIR") + "/" + directory.getPath(), filename);
        if (directory == Directory.CDN) {
            this.filename = filename;
        }
    }

    public LocalFile(@NotNull String pathname) {
        super(System.getenv("ROOT_DIR"), pathname);
    }

    public String cdnGetUrl() {
        if (filename != null) {
            return CDN_ROOT_URL + filename;
        }
        return null;
    }

    public static LocalFile cdnFromUrl(String url) {
        return new LocalFile(Directory.CDN, url.substring(CDN_ROOT_URL.length()));
    }


    public enum Directory {

        CDN("/data/cdn"),
        WELCOME_BACKGROUNDS("/data/welcome_backgrounds"),
        EMOJIS("/data/emojis"),
        RESOURCES("/data/resources"),
        TEMP("temp"),
        DISCORD_BACKUP("/data/discord_backup");

        private final String path;

        Directory(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

    }

}
