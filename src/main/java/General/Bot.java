package General;

import java.io.File;

public class Bot {

    public static boolean isDebug() {
        //The bot is being programmed in Windows and runs on a Linux server, therefore the bot assumes that it runs on debug mode whenever it gets started on a windows device
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static boolean hasUpdate() { return new File("update/Lawliet.jar").exists(); }

}