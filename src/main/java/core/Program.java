package core;

import java.time.Instant;
import core.utils.BotUtil;

public class Program {

    private static boolean stopped = false;
    private static boolean cacheAllMembers = true;
    private final static Instant startTime = Instant.now();

    public static void init() {
        System.out.println("-------------------------------------");
        System.out.println("Production Mode: " + productionMode());
        System.out.println("Cluster ID: " + getClusterId());
        System.out.println("Version: " + BotUtil.getCurrentVersion());
        System.out.println("-------------------------------------");
    }

    public static void onStop() {
        MainLogger.get().info("### STOPPING BOT ###\n{}\nThreads: {}", Console.getInstance().getMemory(), Thread.getAllStackTraces().size());
        stopped = true;
        ShardManager.getInstance().stop();
    }

    public static boolean productionMode() {
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

    public static boolean cacheAllMembers() {
        return cacheAllMembers;
    }

    public static void setCacheAllMembers(boolean cacheAllMembers) {
        Program.cacheAllMembers = cacheAllMembers;
    }

}