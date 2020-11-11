package core.utils;

import core.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

public class SystemUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(BotUtil.class);

    public static int executeProcessSilent(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            Process p = pb.start();
            p.waitFor();
            return p.exitValue();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Could not run process", e);
        }

        return -1;
    }

    public static int executeProcess(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        try {
            Process p = pb.start();
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader reader = new BufferedReader(isr);

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            reader.close();
            isr.close();

            return p.exitValue();
        } catch (IOException e) {
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
