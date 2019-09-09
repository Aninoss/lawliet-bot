package General;

public class Bot {
    public static final boolean TEST_MODE = false; //Warning: The bot uses the non-beta token when running in test mode!
    public static boolean isDebug() {
        //The bot is being programmed in Windows and runs on a Linux server, therefore the bot assumes that it runs on debug mode whenever it gets started on a windows device
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}