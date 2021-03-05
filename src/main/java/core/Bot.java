package core;

import core.utils.BotUtil;
import java.time.Instant;

public class Bot {

    private static boolean stopped = false;
    private final static Instant startTime = Instant.now();

    public static void init() {
        System.out.println("-------------------------------------");
        System.out.println("Production Mode: " + isProductionMode());
        System.out.println("Cluster ID: " + getClusterId());
        System.out.println("Version: " + BotUtil.getCurrentVersion());
        System.out.println("-------------------------------------");
    }

    public static void onStop() {
        MainLogger.get().info(Console.getInstance().getMemory());
        MainLogger.get().info("### STOPPING BOT ###");
        stopped = true;
        ShardManager.getInstance().stop();
    }

    public static boolean isProductionMode() {
        return System.getenv("PRODUCTION").equals("true");
    }

    public static boolean isRunning() {
        return !stopped;
    }

    public static boolean isPublicVersion() {
        return true;
    }

    public static int getClusterId() {
        return Integer.parseInt(System.getenv("CLUSTER"));
    }

    public static Instant getStartTime() {
        return startTime;
    }

}