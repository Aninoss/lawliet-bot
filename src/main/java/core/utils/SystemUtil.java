package core.utils;

import core.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class SystemUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotUtil.class);

    public static int executeProcess(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            Process p = pb.start();
            p.waitFor();
            return p.exitValue();
        } catch (Throwable e) {
            LOGGER.error("Could not run process", e);
        }

        return -1;
    }

    public static void backupDB() {
        if (Bot.isPublicVersion()) {
            String filename = LocalDateTime.now().toString();
            SystemUtil.executeProcess("./backupdb.sh", filename);
            LOGGER.info("Database backup completed!");
        }
    }

}
