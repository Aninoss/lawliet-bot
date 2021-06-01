package core;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import commands.CommandContainer;
import commands.runningchecker.RunningCheckerManager;
import constants.Language;
import core.cache.PatreonCache;
import core.utils.ExceptionUtil;
import core.utils.InternetUtil;
import core.utils.JDAUtil;
import core.utils.TimeUtil;
import events.scheduleevents.events.FisherySurveyResults;
import events.scheduleevents.events.FisheryVoiceChannelObserver;
import javafx.util.Pair;
import modules.repair.MainRepair;
import mysql.DBMain;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import websockets.syncserver.SyncManager;

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

        Thread t = new Thread(this::manageConsole, "Console");
        t.setDaemon(true);
        t.start();
    }

    private void registerTasks() {
        tasks.put("help", this::onHelp);

        tasks.put("actions_servers", this::onActionsServers);
        tasks.put("actions", this::onActions);
        tasks.put("stuck", this::onStuck);
        tasks.put("routes", this::onRoutes);
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
        tasks.put("user_find", this::onUserFind);
        tasks.put("user", this::onUser);
        tasks.put("servers", this::onServers);
        tasks.put("servers_mutual", this::onServersMutual);
        tasks.put("patreon", this::onPatreon);
        tasks.put("patreon_guild", this::onPatreonGuild);
        tasks.put("internet", this::onInternetConnection);
        tasks.put("send_user", this::onSendUser);
        tasks.put("send_channel", this::onSendChannel);
    }

    private void onActionsServers(String[] args) {
        List<Pair<Long, Integer>> guildActionCounts = RestLogger.getInstance().countGuilds(20);
        for (Pair<Long, Integer> guildActionCount : guildActionCounts) {
            MainLogger.get().info("{}: {} requests", guildActionCount.getKey(), guildActionCount.getValue());
        }
    }

    private void onActions(String[] args) {
        MainLogger.get().info("Recent actions in cluster {}: {} requests", Program.getClusterId(), RestLogger.getInstance().count());
    }

    private void onStuck(String[] args) {
        MainLogger.get().info("Cluster {} command stuck counter: {}", Program.getClusterId(), CommandContainer.getInstance().getCommandStuckCounter());
    }

    private void onRoutes(String[] args) {
        int limit = 999;
        if (args.length > 1) {
            limit = Integer.parseInt(args[1]);
        }

        RequestRouteLogger.getInstance().getRoutes()
                .stream()
                .limit(limit)
                .forEach(entry -> MainLogger.get().info("\"{}\": {} requests", entry.getRoute(), entry.getRequests()));
    }

    private void onPatreonFetch(String[] args) {
        PatreonCache.getInstance().requestUpdate();
    }

    private void onSurvey(String[] args) {
        FisherySurveyResults.processCurrentResults();
    }

    private void onRepair(String[] args) {
        int minutes = Integer.parseInt(args[1]);
        ShardManager.getInstance().getConnectedLocalJDAs().forEach(jda -> MainRepair.start(jda, minutes));
        MainLogger.get().info("Repairing cluster {} with {} minutes", Program.getClusterId(), minutes);
    }

    private void onSendChannel(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long channelId = Long.parseLong(args[2]);
        String text = collectArgs(args, 3).replace("\\n", "\n");

        ShardManager.getInstance().getLocalGuildById(serverId)
                .map(guild -> guild.getTextChannelById(channelId))
                .ifPresent(channel -> {
                    MainLogger.get().info("#{}: {}", channel.getName(), text);
                    channel.sendMessage(text).queue();
                });
    }

    private void onSendUser(String[] args) {
        long userId = Long.parseLong(args[1]);
        String text = collectArgs(args, 2).replace("\\n", "\n");
        ShardManager.getInstance().fetchUserById(userId)
                .exceptionally(ExceptionLogger.get())
                .thenAccept(user -> {
                    MainLogger.get().info("@{}: {}", user.getAsTag(), text);
                    JDAUtil.sendPrivateMessage(user, text).queue();
                });
    }

    private void onInternetConnection(String[] args) {
        MainLogger.get().info("Internet connection (Cluster {}): {}", Program.getClusterId(), InternetUtil.checkConnection());
    }

    private void onPatreon(String[] args) {
        long userId = Long.parseLong(args[1]);
        MainLogger.get().info("Patreon stats of user {}: {}", userId, PatreonCache.getInstance().getUserTier(userId, false));
    }

    private void onPatreonGuild(String[] args) {
        long guildId = Long.parseLong(args[1]);
        MainLogger.get().info("{} unlocked: {}", guildId, PatreonCache.getInstance().isUnlocked(guildId));
    }

    private void onServersMutual(String[] args) {
        ShardManager.getInstance().getCachedUserById(Long.parseLong(args[1])).ifPresent(user ->
                ShardManager.getInstance()
                        .getLocalMutualGuilds(user)
                        .stream()
                        .limit(50)
                        .forEach(this::printGuild)
        );
    }

    private void onServers(String[] args) {
        ArrayList<Guild> guilds = new ArrayList<>(ShardManager.getInstance().getLocalGuilds());
        int min = args.length > 1 ? Integer.parseInt(args[1]) : 0;

        guilds.stream()
                .filter(g -> g.getMemberCount() >= min)
                .limit(50)
                .forEach(this::printGuild);
    }

    private void printGuild(Guild guild) {
        int bots = (int) guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();
        MainLogger.get().info(
                "Name: {}; ID: {}; Shard: {}; Cluster: {}; Members: {}; Bots {}",
                guild.getName(),
                guild.getId(),
                guild.getJDA().getShardInfo().getShardId(),
                Program.getClusterId(),
                guild.getMemberCount() - bots,
                bots
        );
    }

    private void onUserFind(String[] args) {
        String username = collectArgs(args, 1);
        AtomicInteger n = new AtomicInteger();
        for (int i = ShardManager.getInstance().getShardIntervalMin(); i <= ShardManager.getInstance().getShardIntervalMax(); i++) {
            ShardManager.getInstance().getJDA(i).map(JDA::getUsers).ifPresent(users -> {
                users.forEach(user -> {
                    if (user.getAsTag().toLowerCase().contains(username.toLowerCase())) {
                        if (n.getAndIncrement() < 20) {
                            MainLogger.get().info("{} => {}", user.getId(), user.getAsTag());
                        }
                    }
                });
            });
        }
    }

    private void onUser(String[] args) {
        long userId = Long.parseLong(args[1]);
        ShardManager.getInstance().fetchUserById(userId)
                .exceptionally(ExceptionLogger.get())
                .thenAccept(user -> MainLogger.get().info("{} => {}", user.getId(), user.getAsTag()));
    }

    private void onFisheryVC(String[] args) {
        long serverId = Long.parseLong(args[1]);
        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(guild -> {
            HashSet<Member> members = new HashSet<>();
            guild.getVoiceChannels().forEach(vc -> members.addAll(FisheryVoiceChannelObserver.getValidVCMembers(vc)));

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
            DBFishery.getInstance().retrieve(serverId).getMemberBean(userId).remove();
            MainLogger.get().info("Fishery user {} from server {} removed", userId, serverId);
        });
    }

    private void onDailyStreak(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(server -> {
            DBFishery.getInstance().retrieve(serverId).getMemberBean(userId).setDailyStreak(value);
            MainLogger.get().info("Changed daily streak value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private void onCoins(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(server -> {
            DBFishery.getInstance().retrieve(serverId).getMemberBean(userId).setCoinsRaw(value);
            MainLogger.get().info("Changed coin value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private void onFish(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getInstance().getLocalGuildById(serverId).ifPresent(server -> {
            DBFishery.getInstance().retrieve(serverId).getMemberBean(userId).setFish(value);
            MainLogger.get().info("Changed fish value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private void onUnban(String[] args) {
        long userId = Long.parseLong(args[1]);
        DBBannedUsers.getInstance().retrieve().getUserIds().remove(userId);
        MainLogger.get().info("User {} unbanned", userId);
    }

    private void onBan(String[] args) {
        long userId = Long.parseLong(args[1]);
        DBBannedUsers.getInstance().retrieve().getUserIds().add(userId);
        MainLogger.get().info("User {} banned", userId);
    }

    private void onThreadsStack(String[] args) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                Exception e = ExceptionUtil.generateForStack(t);
                MainLogger.get().error("\n--- {} - {} ---", Program.getClusterId(), t.getName(), e);
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

        MainLogger.get().info("{} thread/s interrupted in cluster {}", stopped, Program.getClusterId());
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

        MainLogger.get().info("\n--- THREADS FOR CLUSTER {} ({}) ---\n{}\n", Program.getClusterId(), Thread.getAllStackTraces().size(), str);
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
        MainLogger.get().info("Cluster: {} - Shards: {} / {}", Program.getClusterId(), ShardManager.getInstance().getConnectedLocalJDAs().size(), ShardManager.getInstance().getLocalShards());
        for (int i = ShardManager.getInstance().getShardIntervalMin(); i <= ShardManager.getInstance().getShardIntervalMax(); i++) {
            if (!ShardManager.getInstance().jdaIsConnected(i)) {
                MainLogger.get().info("Shard {} is unavailable!", i);
            }
        }
    }

    private void onUptime(String[] args) {
        MainLogger.get().info("Uptime cluster {}: {}", Program.getClusterId(), TimeUtil.getRemainingTimeString(Language.EN.getLocale(), Program.getStartTime(), Instant.now(), false));
    }

    private void onStats(String[] args) {
        MainLogger.get().info(getStats());
    }

    private void onMemory(String[] args) {
        MainLogger.get().info(getMemory());
    }

    private void onQuit(String[] args) {
        MainLogger.get().info("EXIT - Stopping cluster {}", Program.getClusterId());
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

    private String collectArgs(String[] args, int firstIndex) {
        StringBuilder argsString = new StringBuilder();
        for (int i = firstIndex; i < args.length; i++) {
            argsString.append(" ").append(args[i]);
        }
        return argsString.toString().trim();
    }

    public String getMemory() {
        StringBuilder sb = new StringBuilder();
        double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
        sb.append(String.format("Memory of Cluster %d: ", Program.getClusterId()))
                .append(String.format("%1$.2f", memoryUsed))
                .append(" / ")
                .append(String.format("%1$.2f", memoryTotal))
                .append(" MB");

        return sb.toString();
    }

    public String getStats() {
        String header = String.format("--- STATS CLUSTER %d ---", Program.getClusterId());
        StringBuilder sb = new StringBuilder("\n" + header + "\n");

        // heap memory
        double memoryTotal = Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0);
        double memoryUsed = memoryTotal - (Runtime.getRuntime().freeMemory() / (1024.0 * 1024.0));
        sb.append("Heap Memory Memory: ")
                .append(String.format("%1$.2f", memoryUsed))
                .append(" / ")
                .append(String.format("%1$.2f", memoryTotal))
                .append(" MB\n");

        // threads
        sb.append("Threads: ")
                .append(Thread.getAllStackTraces().keySet().size())
                .append("\n");

        // active listeners
        sb.append("Active Listeners: ")
                .append(CommandContainer.getInstance().getListenerSize())
                .append("\n");

        // running commands
        sb.append("Running Commands: ")
                .append(RunningCheckerManager.getInstance().getRunningCommandsMap().size())
                .append("\n");

        sb.append("-".repeat(header.length()))
                .append("\n");
        return sb.toString();
    }


    public interface ConsoleTask {

        void process(String[] args) throws Throwable;

    }

}