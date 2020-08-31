package Core.Utils;

import Constants.Settings;
import Constants.Versions;

public class BotUtil {

    public static String getCurrentVersion() {
        return Versions.VERSIONS[Settings.VERSIONS.length - 1];
    }

}