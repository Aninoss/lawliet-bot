package core;

import java.io.File;

public class ResourceHandler {

    public static String translateRelativePath(String relativePath) {
        return System.getenv("ROOT_DIR") + "/" + relativePath;
    }

    public static File getFileResource(String relativePath) {
        return new File(translateRelativePath(relativePath));
    }

}
