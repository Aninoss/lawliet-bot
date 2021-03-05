package core;

import com.sun.management.OperatingSystemMXBean;
import commands.CommandContainer;
import commands.runningchecker.RunningCheckerManager;
import constants.Locales;
import core.cache.PatreonCache;
import core.utils.*;
import events.scheduleevents.events.SurveyResults;
import modules.FisheryVCObserver;
import modules.repair.MainRepair;
import mysql.DBMain;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import websockets.syncserver.SyncManager;

import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class Console {

    private static final Console instance = new Console();

    public static Console getInstance() {
        return instance;
    }

    private Console() {
        registerTasks();
    }

    private boolean started = false;
    private final HashMap<String, ConsoleTask> tasks = new HashMap<>();

    public void start() {
        if (started) return;
        started = true;

        new CustomThread(this::manageConsole, "console", 1).start();
    }

    private void registerTasks() {
        tasks.put("help", this::onHelp);

        tasks.put("patreon_fetch", this::onPatreonFetch);
        tasks.put("survey", this::onSurvey);
        tasks.put("repair", this::onRepair);
        tasks.put("quit", this::onQuit);
        tasks.put("stats", this::onStats);
        tasks.put("memory", this::onMemory);
        tasks.put("uptime", this::onUptime);
        tasks.put("shards", this::onShards);
        tasks.put("reconnect", this::onReconnect);
        tasks.put("sync_reconnect", this::onSyncReconnect);
        tasks.put("mysql_connect", this::onMySQLConnect);
        tasks.put("threads", this::onThreads);
        tasks.put("threads_stack", this::onThreadsStack);
        tasks.put("threads_interrupt", this::onThreadsInterrupt);
        tasks.put("ban", this::onBan);
        tasks.put("unban", this::onUnban);
        tasks.put("fish", this::onFish);
        tasks.put("coins", this::onCoins);
        tasks.put("daily", this::onDailyStreak);
        tasks.put("delete_fishery_user", this::onDeleteFisheryUser);
        tasks.put("fishery_vc", this::onFisheryVC);
        tasks.put("server", this::onServer);
        tasks.put("leave_server", this::onLeaveServer);
        tasks.put("user", this::onUser);
        tasks.put("clear", this::onClear);
        tasks.put("servers", this::onServers);
        tasks.put("servers_mutual", this::onServersMutual);
        tasks.put("patreon", this::onPatreon);
        tasks.put("internet", this::onInternetConnection);
        tasks.put("send_user", this::onSendUser);
        tasks.put("send_channel", this::onSendChannel);
    }

    private void onPatreonFetch(String[] args) {
        PatreonCache.getInstance().requestUpdate();
    }

    private void onSurvey(String[] args) {
        SurveyResults.processCurrentResults();
    }

    private void onRepair(String[] args) {
        int minutes = Integer.parseInt(args[1]);
        ShardManager.getInstance().getConnectedLocalJDAs().forEach(jda -> MainRepair.start(jda, minutes));
        MainLogger.get().info("Repairing cluster {} with {} minutes", Bot.getClusterId(), minutes);
    }

    private void onSendChannel(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long channelId = Long.parseLong(args[2]);

        StringBuilder message = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            message.append(" ").append(args[i]);
        }
        String text = StringUtil.trimString(message.toString()).replace("\\n", "\n");

        ShardManager.getInstance().getLocalGuildById(serverId)
                .flatMap(server -> Optional.ofNullable(server.getTextChannelById(channelId)))
                .ifPresent(channel -> {
                    MainLogger.get().info("#{}: {}", channel.getName(), text);
                    channel.sendMessage(text).queue();
                });
    }

    private void onSendUser(String[] args) {
        StringBuilder message = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            message.append(" ").append(args[i]);
        }

        long userId = Long.parseLong(args[1]);
        String text = StringUtil.trimString(message.toString()).replace("\\n", "\n");
        ShardManager.getInstance().fetchUserById(userId).join().ifPresent(user -> {
            MainLogger.get().info("@{}: {}", user.getAsTag(), text);
            JDAUtil.sendPrivateMessage(user, text).queue();
        });
    }

    private void onInternetConnection(String[] args) {
        MainLogger.get().info("Internet connection (Cluster {}): {}", Bot.getClusterId(), InternetUtil.checkConnection());
    }

    private void onPatreon(String[] args) {
        long userId = Long.parseLong(args[1]);
        MainLogger.get().info("Patreon stats of user {}: {}", userId, PatreonCache.getInstance().getUserTier(userId));
    }

    private void onServersMutual(String[] args) {
        ShardManager.getInstance().getCachedUserById(Long.parseLong(args[1])).ifPresent(user ->
                ShardManager.getInstance()
                        .getLocalMutualGuilds(user)
                        .forEach(this::printGuild));
    }

    private void onServers(String[] args) {
        ArrayList<Guild> guilds = new ArrayList<>(ShardManager.getInstance().getLocalGuilds());
        int min = args.length > 1 ? Integer.parseInt(args[1]) : 0;

        guilds.stream()
                .filter(g -> g.getMemberCount() >= min)
                .forEach(this::printGuild);
    }

    private void printGuild(Guild guild) {
        int bots = (int) guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();
        MainLogger.get().info(
                "Name: {}; ID: {}; Shard: {}; Cluster: {}; Members: {}; Bots {}",
                guild.getName(),
                guild.getId(),
                guild.getJDA().getShardInfo().getShardId(),
                Bot.getClusterId(),
                guild.getMemberCount() - bots,
                bots
        );
    }

    private void onClear(String[] args) {
        DBMain.getInstance().clearCache();
        MainLogger.get().info("Cache cleared (Cluster {})!", Bot.getClusterId());
    }

    private void onUser(String[] args) {
        long userId = Long.parseLong(args[1]);
        ShardManager.getInstance().fetchUserById(userId).join()
                .ifPresent(user -> MainLogger.get().info("{} => {}", user.getId(), user.getAsTag()));
    }

    private void onFisheryVC(String[] args) {
        long serverId = Long.parseLong(args[1]);
        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(guild -> {
            HashSet<Member> members = new HashSet<>();
            guild.getVoiceChannels().forEach(vc -> members.addAll(FisheryVCObserver.getValidVCMembers(vc)));

            String title = String.format("### VALID VC MEMBERS OF %s ###", guild.getName());
            System.out.println(title);
            members.forEach(member -> System.out.println(member.getUser().getAsTag()));
            System.out.println("-".repeat(title.length()));
        });
    }

    private void onServer(String[] args) {
        long serverId = Long.parseLong(args[1]);
        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(guild ->
                MainLogger.get().info("{} | Members: {} | Owner: {} | Shard {}", guild.getName(), guild.getMemberCount(), guild.getOwner().getUser().getAsTag(), guild.getJDA().getShardInfo().getShardId())
        );
    }

    private void onLeaveServer(String[] args) {
        long serverId = Long.parseLong(args[1]);
        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(server -> {
            server.leave().queue();
            MainLogger.get().info("Left server: {}", server.getName());
        });
    }

    private void onDeleteFisheryUser(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);

        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(server -> {
            DBFishery.getInstance().getBean(serverId).getUserBean(userId).remove();
            MainLogger.get().info("Fishery user {} from server {} removed", userId, serverId);
        });
    }

    private void onDailyStreak(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(server -> {
            DBFishery.getInstance().getBean(serverId).getUserBean(userId).setDailyStreak(value);
            MainLogger.get().info("Changed daily streak value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private void onCoins(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(server -> {
            DBFishery.getInstance().getBean(serverId).getUserBean(userId).setCoinsRaw(value);
            MainLogger.get().info("Changed coin value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private void onFish(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(server -> {
            DBFishery.getInstance().getBean(serverId).getUserBean(userId).setFish(value);
            MainLogger.get().info("Changed fish value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private void onUnban(String[] args) {
        long userId = Long.parseLong(args[1]);
        DBBannedUsers.getInstance().getBean().getUserIds().remove(userId);
        MainLogger.get().info("User {} unbanned", userId);
    }

    private void onBan(String[] args) {
        long userId = Long.parseLong(args[1]);
        DBBannedUsers.getInstance().getBean().getUserIds().add(userId);
        MainLogger.get().info("User {} banned", userId);
    }

    private void onThreadsStack(String[] args) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                Exception e = ExceptionUtil.generateForStack(t);
                MainLogger.get().error("\n--- {} - {} ---", Bot.getClusterId(), t.getName(), e);
            }
        }
    }

    private void onThreadsInterrupt(String[] args) {
        int stopped = 0;

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                t.interrupt();
                stopped++;
            }
        }

        MainLogger.get().info("{} thread/s interrupted in cluster {}", stopped, Bot.getClusterId());
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

        MainLogger.get().info("\n--- THREADS FOR CLUSTER {} ({}) ---\n{}\n", Bot.getClusterId(), Thread.getAllStackTraces().size(), str);
    }

    private void onMySQLConnect(String[] args) {
        try {
            DBMain.getInstance().connect();
        } catch (SQLException e) {
            MainLogger.get().error("Exception", e);
        }
    }

    private void onSyncReconnect(String[] args) {
        SyncManager.getInstance().reconnect();
    }

    private void onReconnect(String[] args) {
        int shardId = Integer.parseInt(args[1]);
        ShardManager.getInstance().reconnectShard(shardId);
    }

    private void onShards(String[] args) {
        MainLogger.get().info("Cluster: {} - Shards: {} / {}", Bot.getClusterId(), ShardManager.getInstance().getConnectedLocalJDAs().size(), ShardManager.getInstance().getLocalShards());
        for (int i = ShardManager.getInstance().getShardIntervalMin(); i <= ShardManager.getInstance().getShardIntervalMax(); i++) {
            if (ShardManager.getInstance().getJDA(i).isEmpty())
                MainLogger.get().info("Shard {} is unavailable!", i);
        }
    }

    private void onUptime(String[] args) {
        MainLogger.get().info("Uptime cluster {}: {}", Bot.getClusterId(), TimeUtil.getRemainingTimeString(new Locale(Locales.EN), Bot.getStartTime(), Instant.now(), false));
    }

    private void onStats(String[] args) {
        MainLogger.get().info(getStats());
    }

    private void onMemory(String[] args) {
        MainLogger.get().info(getMemory());
    }

    private void onQuit(String[] args) {
        MainLogger.get().info("EXIT - Stopping cluster {}", Bot.getClusterId());
        System.exit(0);
    }

    private void onHelp(String[] args) {
        tasks.keySet().stream()
                .filter(key -> !key.equals("help"))
                .sorted()
                .forEach(key -> System.out.println("- " + key));
    }

    private void manageConsole() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.length() > 0) {
                    processInput(line);
                }
            }
        }
    }

    public void processInput(String input) {
        String[] args = input.split(" ");
        ConsoleTask task = tasks.get(args[0]);
        if (task != null) {
            GlobalThreadPool.getExecutorService().submit(() -> {
                try {
                    task.process(args);
                } catch (Throwable throwable) {
                    MainLogger.get().error("Console task {} ended with exception", args[0], throwable);
                }
            });
        } else {
            System.err.printf("No result for \"%s\"\n", args[0]);
        }
    }

    public String getMemory() {
        StringBuilder sb = new StringBuilder();
        double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
        sb.append(String.format("Memory of Cluster %d: ", Bot.getClusterId()))
                .append(String.format("%1$.2f", memoryUsed))
                .append(" / ")
                .append(String.format("%1$.2f", memoryTotal))
                .append(" MB");

        return sb.toString();
    }

    public String getStats() {
        StringBuilder sb = new StringBuilder(String.format("\n--- STATS CLUSTER %d ---\n", Bot.getClusterId()));

        //Memory
        double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
        sb.append("Memory: ")
                .append(String.format("%1$.2f", memoryUsed))
                .append(" / ")
                .append(String.format("%1$.2f", memoryTotal))
                .append(" MB\n");

        //Threads
        sb.append("Threads: ")
                .append(Thread.getAllStackTraces().keySet().size())
                .append("\n");

        //Activities
        sb.append("Activities: ")
                .append(CommandContainer.getInstance().getListenerSize())
                .append("\n");

        //Running Commands
        sb.append("Running Commands: ")
                .append(RunningCheckerManager.getInstance().getRunningCommandsMap().size())
                .append("\n");

        //CPU Usage
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double cpuJvm = osBean.getProcessCpuLoad();
        double cpuTotal = osBean.getSystemCpuLoad();

        sb.append("CPU JVM: ")
                .append(Math.floor(cpuJvm * 1000) / 10)
                .append("%\n");

        sb.append("CPU Total: ")
                .append(Math.floor(cpuTotal * 1000) / 10)
                .append("%\n");

        return sb.toString();
    }


    public interface ConsoleTask {

        void process(String[] args) throws Throwable;

    }

}