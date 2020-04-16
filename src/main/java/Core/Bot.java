package Core;

import java.io.File;

public class Bot {

    private static boolean production = false;
    private static boolean stopped = false;

    public static void setDebug(boolean newProduction) {
        production = newProduction;
        System.out.println("-------------------------------------");
        System.out.println("Production Mode: " + production);
        System.out.println("-------------------------------------");
    }

    public static void stop() { stopped = true; }

    public static boolean isProductionMode() {
        return production;
    }

    public static boolean isStopped() { return stopped; }

    public static boolean hasUpdate() { return new File("update/Lawliet.jar").exists(); }

}