package core;

import commands.CommandContainer;
import commands.runningchecker.RunningCheckerManager;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import core.utils.SystemUtil;
import mysql.DBGiveaway;
import mysql.DBMain;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import websockets.DonationHandler;
import com.sun.management.OperatingSystemMXBean;
import javafx.util.Pair;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class Console {

    private final static Logger LOGGER = LoggerFactory.getLogger(Console.class);

    private static final Console instance = new Console();
    private double maxMemory = 0;
    private boolean started = false;

    private Console() {}

    public static Console getInstance() {
        return instance;
    }

    public void start() {
        if (started) return;
        started = true;

        new CustomThread(this::manageConsole, "console", 1).start();
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
                                LOGGER.info(getStats());
                                break;

                            case "connected":
                                for(int i = 0; i < DiscordApiCollection.getInstance().size(); i++) {
                                    LOGGER.info("Shard {}: {}", i, DiscordApiCollection.getInstance().shardIsConnected(i));
                                }
                                break;

                            case "reconnect":
                                try {
                                    int shardId = Integer.parseInt(arg);
                                    DiscordApiCollection.getInstance().reconnectShard(shardId);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not reconnect shard", e);
                                }
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

                                    LOGGER.info("\n--- THREADS ({}) ---\n{}\n", Thread.getAllStackTraces().size(), str);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not list threads", e);
                                }

                                break;

                            case "donation_insert":
                                try {
                                    long userId = Long.parseLong(arg.split(" ")[0]);
                                    double usDollars = Double.parseDouble(arg.split(" ")[1]);
                                    DonationHandler.addBonus(userId, usDollars);
                                    LOGGER.info("{} dollars donated by {}", usDollars, userId);
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
                                    DBFishery.getInstance().getBean(serverId).getUserBean(userId).setCoinsRaw(value);
                                    LOGGER.info("Changed coin value (server: {}; user: {}) to {}", serverId, userId, value);
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage user fish", e);
                                }
                                break;

                            case "daily_streak":
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
                                DiscordApiCollection.getInstance().getServerById(serverId).ifPresent(server -> System.out.println(server.getName() + " | " + server.getMemberCount() + " | Owner: " + server.getOwner().get().getDiscriminatedName()));
                                break;

                            case "clear":
                                DBMain.getInstance().clearCache();
                                LOGGER.info("Cache cleared!");
                                break;

                            case "fonts_reload":
                                FontContainer.getInstance().reload();
                                LOGGER.info("Fonts reloaded!");
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

                            case "donation_status":
                                try {
                                    long userId = Long.parseLong(arg.split(" ")[0]);
                                    LOGGER.info("Donation stats of user {}: {}", userId, PatreonCache.getInstance().getPatreonLevel(userId));
                                } catch (Throwable e) {
                                    LOGGER.error("Could not manage donation", e);
                                }
                                break;

                            case "connection":
                                try {
                                    LOGGER.info("Internet connection: {}", InternetUtil.checkConnection());
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
                                                LOGGER.info("{} ({}) - Patreon: {}", user.getDiscriminatedName(), user.getId(), PatreonCache.getInstance().getPatreonLevel(user.getId()));
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
                                            LOGGER.info("MESSAGE SENT SUCCESS");
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

    public String getStats() {
        StringBuilder sb = new StringBuilder("\n--- STATS ---\n");

        //Memory
        double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
        sb.append("Memory: ").append(String.format("%1$.2f", memoryUsed) + " / " + String.format("%1$.2f", memoryTotal) + " MB").append("\n");

        //Max Memory
        maxMemory = Math.max(maxMemory, memoryUsed);
        sb.append("Max Memory: ").append(String.format("%1$.2f", maxMemory) + " / " + String.format("%1$.2f", memoryTotal) + " MB").append("\n");

        //Threads
        sb.append("Threads: ").append(Thread.getAllStackTraces().keySet().size()).append("\n");

        LOGGER.info("RAM: {} & {}", memoryUsed, maxMemory);
        LOGGER.info("Threads: : {}",  Thread.getAllStackTraces().keySet().size());

        //Activities
        sb.append("Activities: ").append(CommandContainer.getInstance().getActivitiesSize()).append("\n");

        //Running Commands
        sb.append("Running Commands: ").append(RunningCheckerManager.getInstance().getRunningCommandsMap().size()).append("\n");

        //CPU Usage
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double cpuJvm = osBean.getProcessCpuLoad();
        double cpuTotal = osBean.getSystemCpuLoad();

        sb.append("CPU JVM: ").append(Math.floor(cpuJvm * 1000) / 10 + "%").append("\n");
        sb.append("CPU Total: ").append(Math.floor(cpuTotal * 1000) / 10 + "%").append("\n");

        return sb.toString();
    }

    public double getMaxMemory() { return maxMemory; }

    public void setMaxMemory(double maxMemory) {
        this.maxMemory = maxMemory;
    }

}