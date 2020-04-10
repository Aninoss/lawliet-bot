package Core;

import java.io.File;

public class Bot {

    private static boolean production = false;
    private final boolean DEBUG_IN_PRODUCTION_ENVIRONMENT = true;

    public static void setDebug(boolean newProduction) {
        production = newProduction;
        System.out.println("-------------------------------------");
        System.out.println("Production Mode: " + production);
        System.out.println("-------------------------------------");
    }

    public static boolean isProductionMode() {
        return production;
    }

    public static boolean hasUpdate() { return new File("update/Lawliet.jar").exists(); }

}