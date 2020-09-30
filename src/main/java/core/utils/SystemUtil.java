package core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Optional;

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

    public static Optional<File> backupDB() {
        String filename = LocalDateTime.now().toString();
        int code = SystemUtil.executeProcess("./backupdb.sh", filename);
        if (code != 0) {
            LOGGER.error("Could not backup db! Exit code {}", code);
            return Optional.empty();
        }
        return Optional.of(new File(String.format("backups/%s.sql", filename)));
    }

}
