package Core;

import CommandSupporters.CommandContainer;
import CommandSupporters.RunningCommands.RunningCommandManager;
import Core.Utils.BotUtil;
import Core.Utils.InternetUtil;
import Core.Utils.StringUtil;
import Core.Utils.SystemUtil;
import MySQL.DBGiveaway;
import MySQL.DBMain;
import MySQL.Modules.BannedUsers.BannedUsersBean;
import MySQL.Modules.BannedUsers.DBBannedUsers;
import MySQL.Modules.FisheryUsers.DBFishery;
import ServerStuff.DonationHandler;
import com.sun.management.OperatingSystemMXBean;
import javafx.util.Pair;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class Console {

    final static Logger LOGGER = LoggerFactory.getLogger(Console.class);
    private static final Console instance = new Console();
    private double maxMemory = 0;
    private double traffic = -1;

    private Console() {}

    public static Console getInstance() {
        return instance;
    }

    public void start() {
        new CustomThread(this::manageConsole, "console", 1).start();
        new CustomThread(this::startAutoPrint, "console_autostats", 1).start();
        new CustomThread(this::trackMemory, "console_memorytracker", 1).start();
    }

    private void manageConsole() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                if (br.ready()) {

                    String s = br.readLine();
                    String command = s;
                    String arg = "";
                    if (s != null) {
                        if (command.contains(" ")) {
                            command = command.split(" ")[0];
                            arg = s.substring(command.length() + 1);
                        }
                        switch (command) {
                            case "quit":
                                LOGGER.info("EXIT - User commanded exit");
                                System.exit(0);
                                break;

                            case "stats":
                                System.out.println(getStats());
                                break;

                            case "connected":
                                System.out.println(DiscordApiCollection.getInstance().allShardsConnected());
                                break;

                            case "threads":
                                try {
                                    StringBuilder sb = new StringBuilder();

                                    for (Thread t : Thread.getAllStackTraces().keySet()) {
                                        if (arg.isEmpty() || t.getName().matches(arg)) {
                                            sb.append(t.getName()).append(", ");
                                        }
                                    }

                                    String str = sb.toString();
                                    if (str.length() >= 2) str = str.substring(0, str.length() - 2);

                                    System.out.println("\n--- THREADS (" + Thread.getAllStackTraces().size() + ") ---");
                                    System.out.println(str + "\n");
                                } catch (Throwable e) {
                                    LOGGER.error("Could not list threads", e);
                                }

                                break;

                            case "donation_insert":
                                try {
                                    long userId = Long.parseLong(arg.split(" ")[0]);
                                    double usDollars = Double.parseDouble(arg.split(" ")[1]);
                                    DonationHandler.addBonus(userId, usDollars);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage donation", e);
                                }
                                break;

                            case "ban":
                                try {
                                    long userId = Long.parseLong(arg);
                                    DBBannedUsers.getInstance().getBean().getUserIds().add(userId);
                                    LOGGER.info("User {} banned", userId);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage ban", e);
                                }
                                break;

                            case "unban":
                                try {
                                    long userId = Long.parseLong(arg);
                                    DBBannedUsers.getInstance().getBean().getUserIds().remove(userId);
                                    LOGGER.info("User {} unbanned", userId);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage unban", e);
                                }
                                break;

                            case "fish":
                                try {
                                    long serverId = Long.parseLong(arg.split(" ")[0]);
                                    long userId = Long.parseLong(arg.split(" ")[1]);
                                    long value = Long.parseLong(arg.split(" ")[2]);
                                    DBFishery.getInstance().getBean(serverId).getUserBean(userId).setFish(value);
                                    LOGGER.info("Changed fish value (server: {}; user: {}) to {}", serverId, userId, value);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage user fish", e);
                                }
                                break;

                            case "coins":
                                try {
                                    long serverId = Long.parseLong(arg.split(" ")[0]);
                                    long userId = Long.parseLong(arg.split(" ")[1]);
                                    long value = Long.parseLong(arg.split(" ")[2]);
                                    DBFishery.getInstance().getBean(serverId).getUserBean(userId).setCoins(value);
                                    LOGGER.info("Changed coin value (server: {}; user: {}) to {}", serverId, userId, value);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage user fish", e);
                                }
                                break;

                            case "dailystreak":
                                try {
                                    long serverId = Long.parseLong(arg.split(" ")[0]);
                                    long userId = Long.parseLong(arg.split(" ")[1]);
                                    int value = Integer.parseInt(arg.split(" ")[2]);
                                    DBFishery.getInstance().getBean(serverId).getUserBean(userId).setDailyStreak(value);
                                    LOGGER.info("Changed daily streak value (server: {}; user: {}) to {}", serverId, userId, value);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage user fish", e);
                                }
                                break;

                            case "delete":
                            case "remove":
                                try {
                                    long serverId = Long.parseLong(arg.split(" ")[0]);
                                    long userId = Long.parseLong(arg.split(" ")[1]);
                                    DBFishery.getInstance().getBean(serverId).getUserBean(userId).remove();
                                    LOGGER.info("Fishery user {} from server {} removed", userId, serverId);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not remove fishery user", e);
                                }
                                break;

                            case "server":
                                long serverId = Long.parseLong(arg);
                                DiscordApiCollection.getInstance().getServerById(serverId).ifPresent(server -> System.out.println(server.getName() + " | " + server.getMemberCount()));
                                break;

                            case "clear":
                                DBMain.getInstance().clearCache();
                                System.out.println("Cache cleared!");
                                break;

                            case "backup":
                                SystemUtil.backupDB();
                                break;

                            case "servers":
                                LOGGER.info("--- SERVERS ---");
                                ArrayList<Server> servers = new ArrayList<>(DiscordApiCollection.getInstance().getServers());
                                servers.sort((s1, s2) -> Integer.compare(s2.getMemberCount(), s1.getMemberCount()));
                                int limit = servers.size();
                                if (!arg.isEmpty()) limit = Math.min(servers.size(), Integer.parseInt(arg));

                                for(int i = 0; i < limit; i++) {
                                    Server server = servers.get(i);
                                    int bots = (int) server.getMembers().stream().filter(User::isBot).count();
                                    LOGGER.info("{} (ID: {}): {} Members & {} Bots",
                                            server.getName(),
                                            server.getId(),
                                            server.getMemberCount() - bots,
                                            bots
                                    );
                                }
                                LOGGER.info("---------------");
                                break;

                            case "donation":
                                try {
                                    long userId = Long.parseLong(arg.split(" ")[0]);
                                    System.out.println(PatreonCache.getInstance().getPatreonLevel(userId));
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage donation", e);
                                }
                                break;

                            case "connection":
                                try {
                                    LOGGER.info("Internet Connection: {}", InternetUtil.checkConnection());
                                } catch (Throwable e) {
                                    LOGGER.error("Could not check connection", e);
                                }
                                break;

                            case "giveaway":
                                try {
                                    LOGGER.info("### GIVEAWAY RESULTS ###");
                                    for(Pair<Long, Long> slot : DBGiveaway.getGiveawaySlots()) {
                                        DiscordApiCollection.getInstance().getServerById(slot.getKey()).ifPresent(server -> {
                                            if (server.getMembers().stream().filter(user -> !user.isBot()).count() >= 10 &&
                                                server.getMembers().stream().anyMatch(user -> user.getId() == slot.getValue())
                                            ) {
                                                User user = server.getMemberById(slot.getValue()).get();
                                                try {
                                                    LOGGER.info("{} ({}) - Patreon: {}", user.getDiscriminatedName(), user.getId(), PatreonCache.getInstance().getPatreonLevel(user.getId()));
                                                } catch (ExecutionException e) {
                                                    LOGGER.error("Exception for user with id {}", user.getId(), e);
                                                }
                                            }
                                        });
                                    }
                                } catch (Throwable e) {
                                    LOGGER.error("Could not check connection", e);
                                }
                                break;

                            case "send":
                                try {
                                    String userIdString = arg.split(" ")[0];
                                    long userId = Long.parseLong(userIdString);
                                    String text = StringUtil.trimString(arg.substring(userIdString.length())).replace("\\n", "\n");
                                    DiscordApiCollection.getInstance().getUserById(userId).ifPresent(user -> {
                                        try {
                                            LOGGER.info(">{} ({}): {}", user.getDiscriminatedName(), user.getId(), text);
                                            user.sendMessage(text).get();
                                            LOGGER.info("SUCCESS");
                                        } catch (InterruptedException | ExecutionException e) {
                                            LOGGER.error("Exception", e);
                                        }
                                    });
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage donation", e);
                                }
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Unexpected exception", e);
            }
        }
    }

    private void startAutoPrint() {
        try {
            while (true) {
                Thread.sleep(60 * 1000);
                System.out.println(getStats());
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
        }
    }

    private void trackMemory() {
        try {
            while (true) {
                double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
                double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
                if (memoryUsed > maxMemory) {
                    maxMemory = memoryUsed;
                    LOGGER.debug("Max Memory: {} / {}", memoryUsed, memoryTotal);
                }
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
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

        //ExceptionHandler.showInfoLog(String.format("RAM: %f / %f", memoryUsed, maxMemory));
        //ExceptionHandler.showInfoLog(String.format("Threads: %d", Thread.getAllStackTraces().keySet().size()));

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

    public double getMaxMemory() { return maxMemory; }

}