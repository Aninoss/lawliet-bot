package core;

import java.io.File;
import org.jetbrains.annotations.NotNull;

public class LocalFile extends File {

    public LocalFile(@NotNull String pathname) {
        super(System.getenv("ROOT_DIR"), pathname);
    }

}
