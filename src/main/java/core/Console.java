package core;

import com.sun.management.OperatingSystemMXBean;
import commands.CommandContainer;
import commands.runningchecker.RunningCheckerManager;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import core.utils.SystemUtil;
import modules.FisheryVCObserver;
import mysql.DBMain;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.DonationHandler;
import websockets.webcomserver.WebComServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

public class Console {

    private final static Logger LOGGER = LoggerFactory.getLogger(Console.class);

    private static final Console instance = new Console();

    public static Console getInstance() {
        return instance;
    }

    private Console() {
        registerTasks();
    }

    private double maxMemory = 0;
    private boolean started = false;
    private final HashMap<String, ConsoleTask> tasks = new HashMap<>();

    public void start() {
        if (started) return;
        started = true;

        new CustomThread(this::manageConsole, "console", 1).start();
    }

    private void registerTasks() {
        tasks.put("help", this::onHelp);

        tasks.put("webcom_start", this::onWebComStart);
        tasks.put("webcom_stop", this::onWebComStop);
        tasks.put("webcom", this::onWebCom);
        tasks.put("eval", this::onEval);
        tasks.put("eval_file", this::onEvalFile);
        tasks.put("quit", this::onQuit);
        tasks.put("stats", this::onStats);
        tasks.put("shards", this::onShards);
        tasks.put("reconnect", this::onReconnect);
        tasks.put("threads", this::onThreads);
        tasks.put("threads_stop", this::onThreadStop);
        tasks.put("stop_threads", this::onThreadStop);
        tasks.put("threads_kill", this::onThreadStop);
        tasks.put("kill_threads", this::onThreadStop);
        tasks.put("donation", this::onDonationInsert);
        tasks.put("ban", this::onBan);
        tasks.put("unban", this::onUnban);
        tasks.put("fish", this::onFish);
        tasks.put("coins", this::onCoins);
        tasks.put("daily", this::onDailyStreak);
        tasks.put("delete_fishery_user", this::onDeleteFisheryUser);
        tasks.put("remove_fishery_user", this::onDeleteFisheryUser);
        tasks.put("fishery_vc", this::onFisheryVC);
        tasks.put("server", this::onServer);
        tasks.put("user", this::onUser);
        tasks.put("clear", this::onClear);
        tasks.put("fonts", this::onReloadFonts);
        tasks.put("backup", this::onBackup);
        tasks.put("servers", this::onServers);
        tasks.put("users", this::onUsers);
        tasks.put("patreon", this::onPatreon);
        tasks.put("patreon_set", this::onPatreonSet);
        tasks.put("internet", this::onInternetConnection);
        tasks.put("send_user", this::onSendUser);
        tasks.put("send_server", this::onSendChannel);
        tasks.put("send_channel", this::onSendChannel);
    }

    private void onWebComStart(String[] args) {
        int port = 15744;
        if (args.length > 1) port = Integer.parseInt(args[1]);
        WebComServer.getInstance().start(port);
    }

    private void onWebComStop(String[] args) {
        WebComServer.getInstance().stop();
        LOGGER.info("WebCom server stopped");
    }

    private void onWebCom(String[] args) {
        LOGGER.info("WebCom connection: {}", WebComServer.getInstance().isConnected());
    }

    private void onEval(String[] args) throws Exception {
        StringBuilder message = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            message.append(" ").append(args[i]);
        }

        int retValue = new CodeExecutor().eval(StringUtil.trimString(message.toString()));
        System.out.printf("### CODE EXITED WITH %d ###\n", retValue);
    }

    private void onEvalFile(String[] args) throws Exception {
        int retValue = new CodeExecutor().evalFile("data/CodeRuntime.java");
        System.out.printf("### CODE EXITED WITH %d ###\n", retValue);
    }

    private void onSendChannel(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long channelId = Long.parseLong(args[2]);

        StringBuilder message = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            message.append(" ").append(args[i]);
        }
        String text = StringUtil.trimString(message.toString()).replace("\\n", "\n");

        DiscordApiCollection.getInstance().getServerById(serverId)
                .flatMap(server -> server.getTextChannelById(channelId))
                .ifPresent(channel -> {
                    LOGGER.info(">{} (#{}): {}", channel.getName(), channel.getId(), text);
                    channel.sendMessage(text).exceptionally(ExceptionLogger.get());
                });
    }

    private void onSendUser(String[] args) {
        StringBuilder message = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            message.append(" ").append(args[i]);
        }

        long userId = Long.parseLong(args[1]);
        String text = StringUtil.trimString(message.toString()).replace("\\n", "\n");
        DiscordApiCollection.getInstance().getUserById(userId).ifPresent(user -> {
            LOGGER.info(">{} ({}): {}", user.getDiscriminatedName(), user.getId(), text);
            user.sendMessage(text).exceptionally(ExceptionLogger.get());
        });
    }

    private void onInternetConnection(String[] args) {
        LOGGER.info("Internet connection: {}", InternetUtil.checkConnection());
    }

    private void onPatreon(String[] args) {
        long userId = Long.parseLong(args[1]);
        LOGGER.info("Patreon stats of user {}: {}", userId, PatreonCache.getInstance().getPatreonLevel(userId));
    }

    private void onPatreonSet(String[] args) {
        long userId = Long.parseLong(args[1]);
        int level = Integer.parseInt(args[2]);
        PatreonCache.getInstance().setPatreonLevel(userId, level);
        LOGGER.info("Patreon stats of user {} set: {}", userId, PatreonCache.getInstance().getPatreonLevel(userId));
    }

    private void onUsers(String[] args) {
        LOGGER.info("Total users: " + DiscordApiCollection.getInstance().getUserIds().size());
    }

    private void onServers(String[] args) {
        LOGGER.info("--- SERVERS ---");
        ArrayList<Server> servers = new ArrayList<>(DiscordApiCollection.getInstance().getServers());
        servers.sort((s1, s2) -> Integer.compare(s2.getMemberCount(), s1.getMemberCount()));
        int limit = servers.size();
        if (args.length >= 2)
            limit = Math.min(servers.size(), Integer.parseInt(args[1]));

        for (int i = 0; i < limit; i++) {
            Server server = servers.get(i);
            int bots = (int) server.getMembers().stream().filter(User::isBot).count();
            LOGGER.info(
                    "{} (ID: {}): {} Members & {} Bots",
                    server.getName(),
                    server.getId(),
                    server.getMemberCount() - bots,
                    bots
            );
        }
        LOGGER.info("---------------");
    }

    private void onBackup(String[] args) {
        SystemUtil.backupDB();
        System.out.println("Backup completed!");
    }

    private void onReloadFonts(String[] args) {
        FontContainer.getInstance().reload();
        LOGGER.info("Fonts reloaded!");
    }

    private void onClear(String[] args) {
        DBMain.getInstance().clearCache();
        LOGGER.info("Cache cleared!");
    }

    private void onUser(String[] args) {
        long userId = Long.parseLong(args[1]);
        DiscordApiCollection.getInstance().getUserById(userId).ifPresent(user -> System.out.println(user.getDiscriminatedName()));
    }

    private void onFisheryVC(String[] args) {
        long serverId = Long.parseLong(args[1]);
        DiscordApiCollection.getInstance().getServerById(serverId).ifPresent(server -> {
            HashSet<User> users = new HashSet<>();
            server.getVoiceChannels().forEach(vc -> {
                users.addAll(FisheryVCObserver.getValidVCUsers(server, vc));
            });

            String title = String.format("### VALID VC MEMBERS OF %s ###", server.getName());
            System.out.println(title);
            users.forEach(user -> System.out.println(user.getDiscriminatedName()));
            System.out.println("-".repeat(title.length()));
        });
    }

    private void onServer(String[] args) {
        long serverId = Long.parseLong(args[1]);
        DiscordApiCollection.getInstance().getServerById(serverId).ifPresent(server -> LOGGER.info("{} | Members: {} | Owner: {} | Shard {}", server.getName(), server.getMemberCount(), server.getOwner().get().getDiscriminatedName(), server.getApi().getCurrentShard()));
    }

    private void onDeleteFisheryUser(String[] args) throws ExecutionException {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        DBFishery.getInstance().getBean(serverId).getUserBean(userId).remove();
        LOGGER.info("Fishery user {} from server {} removed", userId, serverId);
    }

    private void onDailyStreak(String[] args) throws ExecutionException {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);
        DBFishery.getInstance().getBean(serverId).getUserBean(userId).setDailyStreak(value);
        LOGGER.info("Changed daily streak value (server: {}; user: {}) to {}", serverId, userId, value);
    }

    private void onCoins(String[] args) throws ExecutionException {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);
        DBFishery.getInstance().getBean(serverId).getUserBean(userId).setCoins(value);
        LOGGER.info("Changed coin value (server: {}; user: {}) to {}", serverId, userId, value);
    }

    private void onFish(String[] args) throws ExecutionException {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);
        DBFishery.getInstance().getBean(serverId).getUserBean(userId).setFish(value);
        LOGGER.info("Changed fish value (server: {}; user: {}) to {}", serverId, userId, value);
    }

    private void onUnban(String[] args) throws SQLException {
        long userId = Long.parseLong(args[1]);
        DBBannedUsers.getInstance().getBean().getUserIds().remove(userId);
        LOGGER.info("User {} unbanned", userId);
    }

    private void onBan(String[] args) throws SQLException {
        long userId = Long.parseLong(args[1]);
        DBBannedUsers.getInstance().getBean().getUserIds().add(userId);
        LOGGER.info("User {} banned", userId);
    }

    private void onDonationInsert(String[] args) throws SQLException, InterruptedException {
        long userId = Long.parseLong(args[1]);
        double usDollars = Double.parseDouble(args[2]);
        DonationHandler.addBonus(userId, usDollars);
        LOGGER.info("{} dollars donated by {}", usDollars, userId);
    }

    private void onThreadStop(String[] args) {
        int stopped = 0;

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                t.interrupt();
                stopped++;
            }
        }

        LOGGER.info("{} thread/s interrupted", stopped);
    }

    private void onThreads(String[] args) {
        StringBuilder sb = new StringBuilder();

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                sb.append(t.getName()).append(", ");
            }
        }

        String str = sb.toString();
        if (str.length() >= 2) str = str.substring(0, str.length() - 2);

        LOGGER.info("\n--- THREADS ({}) ---\n{}\n", Thread.getAllStackTraces().size(), str);
    }

    private void onReconnect(String[] args) {
        int shardId = Integer.parseInt(args[1]);
        DiscordApiCollection.getInstance().reconnectShard(shardId);
    }

    private void onShards(String[] args) {
        for (int i = 0; i < DiscordApiCollection.getInstance().size(); i++) {
            LOGGER.info("Shard {}: {} ({} unavailable)", i, DiscordApiCollection.getInstance().shardIsConnected(i), DiscordApiCollection.getInstance().getApis().get(i).getUnavailableServers().size());
        }
    }

    private void onStats(String[] args) {
        LOGGER.info(getStats());
    }

    private void onQuit(String[] args) {
        LOGGER.info("EXIT - User commanded exit");
        SystemUtil.backupDB();
        System.exit(0);
    }

    private void onHelp(String[] args) {
        tasks.keySet().stream()
                .filter(key -> !key.equals("help"))
                .sorted()
                .forEach(key -> System.out.println("- " + key));
    }

    private void manageConsole() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                if (br.ready()) {
                    String[] args = br.readLine().split(" ");
                    ConsoleTask task = tasks.get(args[0]);
                    if (task != null) {
                        new CustomThread(() -> {
                            try {
                                task.process(args);
                            } catch (Throwable throwable) {
                                LOGGER.error("Console task {} endet with exception", args[0], throwable);
                            }
                        }, "console_task", 1).start();
                    } else {
                        System.err.printf("No result for \"%s\"\n", args[0]);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Unexpected console exception", e);
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

        //LOGGER.info("RAM: {} & {}", memoryUsed, maxMemory);
        //LOGGER.info("Threads: : {}", Thread.getAllStackTraces().keySet().size());

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

    public double getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(double maxMemory) {
        this.maxMemory = maxMemory;
    }


    public interface ConsoleTask {

        void process(String[] args) throws Throwable;

    }

}