package core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

}
