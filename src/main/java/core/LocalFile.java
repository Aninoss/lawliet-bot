package core;

import java.io.File;
import org.jetbrains.annotations.NotNull;

public class LocalFile extends File {

    public LocalFile(Directory directory, @NotNull String filename) {
        super(System.getenv("ROOT_DIR") + "/" + directory.getPath(), filename);
    }

    public LocalFile(@NotNull String pathname) {
        super(System.getenv("ROOT_DIR"), pathname);
    }


    public enum Directory {

        CDN("/data/cdn"),
        YOUTUBE_DL("/data/youtube-dl"),
        WELCOME_BACKGROUNDS("/data/welcome_backgrounds"),
        RESOURCES("/data/resources"),
        TEMP("temp");


        private final String path;

        Directory(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

}
