package General;

public class Bot {

    private static boolean restartPending = false;

    public static boolean isDebug() {
        //The bot is being programmed in Windows and runs on a Linux server, therefore the bot assumes that it runs on debug mode whenever it gets started on a windows device
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public static void setRestartPending() {
        Bot.restartPending = true;
    }

    public static boolean isRestartPending() {
        return restartPending;
    }
}