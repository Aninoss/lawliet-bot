package Core.Utils;

import Constants.Versions;

public class BotUtil {

    public static String getCurrentVersion() {
        return Versions.VERSIONS[Versions.VERSIONS.length - 1];
    }

}