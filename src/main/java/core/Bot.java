package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Bot {

    private final static Logger LOGGER = LoggerFactory.getLogger(Bot.class);

    private static boolean production = false;
    private static boolean stopped = false;

    public static void setDebug(boolean newProduction) {
        production = newProduction;
        System.out.println("-------------------------------------");
        System.out.println("Production Mode: " + production);
        System.out.println("-------------------------------------");
    }

    public static void onStop() {
        LOGGER.info("### STOPPING BOT ###");
        stopped = true;
        DiscordApiCollection.getInstance().stop();
    }

    public static boolean isProductionMode() {
        return production;
    }

    public static boolean isRunning() { return !stopped; }

    public static boolean hasUpdate() { return new File("update/Lawliet.jar").exists(); }

}