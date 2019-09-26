package General;

import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import General.RunningCommands.RunningCommand;
import General.RunningCommands.RunningCommandManager;
import ServerStuff.SIGNALTRANSMITTER.SIGNALTRANSMITTER;
import com.sun.management.OperatingSystemMXBean;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class Console {

    private static Console instance = new Console();
    private double maxMemory = 0;
    private double traffic = -1;

    private Console() {}

    public static Console getInstance() {
        return instance;
    }

    public void start() {
        new Thread(this::manageConsole).start();
        new Thread(this::startAutoPrint).start();
        new Thread(this::trackMemory).start();
    }

    private void manageConsole() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                if (br.ready()) {
                    String s = br.readLine();
                    if (s != null) {
                        switch (s) {
                            case "quit":
                                System.exit(0);
                                break;

                            case "stats":
                                System.out.println(getStats());
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startAutoPrint() {
        while (true) {
            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(getStats());
        }
    }

    private void trackMemory() {
        while(true) {
            double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
            double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
            maxMemory = Math.max(maxMemory, memoryUsed);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String getStats() {
        StringBuilder sb = new StringBuilder("\n--- STATS ---\n");

        //Traffic
        sb.append("Traffic: ").append(traffic + " GB").append("\n");

        //Memory
        double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
        sb.append("Memory: ").append(String.format("%1$.2f", memoryUsed) + " / " + String.format("%1$.2f", memoryTotal) + " MB").append("\n");

        //Max Memory
        maxMemory = Math.max(maxMemory, memoryUsed);
        sb.append("Max Memory: ").append(String.format("%1$.2f", maxMemory) + " / " + String.format("%1$.2f", memoryTotal) + " MB").append("\n");

        //Threads
        sb.append("Threads: ").append(Thread.getAllStackTraces().keySet().size()).append("\n");

        //Activities
        sb.append("Activities: ").append(CommandContainer.getInstance().getActivitiesSize()).append("\n");

        //Running Commands
        sb.append("Running Commands: ").append(RunningCommandManager.getInstance().getRunningCommands().size()).append("\n");

        //CPU Usage
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double cpuJvm = osBean.getProcessCpuLoad();
        double cpuTotal = osBean.getSystemCpuLoad();

        sb.append("CPU JVM: ").append(Math.floor(cpuJvm * 1000) / 10 + "%").append("\n");
        sb.append("CPU Total: ").append(Math.floor(cpuTotal * 1000) / 10 + "%").append("\n");

        return sb.toString();
    }

    public void setTraffic(double traffic) {
        this.traffic = traffic;
    }

}