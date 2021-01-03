package core.utils;

import core.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotUtil.class);

    public static void rsyncPush(String fileSource, String fileDestination) {
        executeProcess("./rsync_push.sh", fileSource, fileDestination);
    }

    public static void rsyncDelete(String file) {
        executeProcess("./rsync_delete.sh", file);
    }

    public static void downloadYouTubeVideo(String id) {
        executeProcess(Bot.isProductionMode() ? "./ytmp3.sh" : "ytmp3.bat", id);
    }

    private static void executeProcess(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (Throwable e) {
            LOGGER.error("Could not run process", e);
        }
    }

}
