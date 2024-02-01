package core;

import commands.CommandContainer;
import commands.SlashCommandManager;
import commands.runningchecker.RunningCheckerManager;
import constants.AssetIds;
import constants.Language;
import core.cache.PatreonCache;
import core.featurelogger.FeatureLogger;
import core.utils.*;
import events.scheduleevents.events.*;
import javafx.util.Pair;
import modules.SupportTemplates;
import modules.casinologs.CasinoLogCache;
import modules.fishery.Fishery;
import modules.repair.MainRepair;
import modules.schedulers.AlertScheduler;
import mysql.MySQLManager;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.user.Txt2ImgEntity;
import mysql.hibernate.entity.user.UserEntity;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Console {

    private static final HashMap<String, ConsoleTask> tasks = new HashMap<>();

    static {
        registerTasks();
    }

    public static void start() {
        Thread t = new Thread(Console::manageConsole, "Console");
        t.setDaemon(true);
        t.start();
    }

    private static void registerTasks() {
        tasks.put("help", Console::onHelp);

        tasks.put("bot_logs_cleanup", Console::onBotLogsCleanUp);
        tasks.put("fishery_copy", Console::onFisheryCopy);
        tasks.put("featurelogging", Console::onFeatureLogging);
        tasks.put("txt2img_ban", Console::onTxt2ImgBan);
        tasks.put("casino_logs", Console::onCasinoLogs);
        tasks.put("tickets_auto_close", Console::onTicketsAutoClose);
        tasks.put("slash_update", Console::onSlashUpdate);
        tasks.put("update_lawliet_support_commands", Console::onUpdateLawlietSupportCommands);
        tasks.put("gdpr", Console::onGdpr);
        tasks.put("dev_votes_results", Console::onDevVotesResults);
        tasks.put("dev_votes", Console::onDevVotes);
        tasks.put("mute_refresh", Console::onMuteRefresh);
        tasks.put("clean_guilds", Console::onCleanGuilds);
        tasks.put("alerts_reset", Console::onAlertsReset);
        tasks.put("reminder_daily", Console::onReminderDaily);
        tasks.put("actions_servers", Console::onActionsServers);
        tasks.put("actions", Console::onActions);
        tasks.put("stuck", Console::onStuck);
        tasks.put("clear_routes", Console::onClearRoutes);
        tasks.put("routes", Console::onRoutes);
        tasks.put("patreon_fetch", Console::onPatreonFetch);
        tasks.put("survey", Console::onSurvey);
        tasks.put("repair", Console::onRepair);
        tasks.put("quit", Console::onQuit);
        tasks.put("stats", Console::onStats);
        tasks.put("memory", Console::onMemory);
        tasks.put("uptime", Console::onUptime);
        tasks.put("shards", Console::onShards);
        tasks.put("reconnect", Console::onReconnect);
        tasks.put("mysql_connect", Console::onMySQLConnect);
        tasks.put("threads", Console::onThreads);
        tasks.put("threads_stack", Console::onThreadsStack);
        tasks.put("threads_interrupt", Console::onThreadsInterrupt);
        tasks.put("ban", Console::onBan);
        tasks.put("unban", Console::onUnban);
        tasks.put("fish", Console::onFish);
        tasks.put("coins", Console::onCoins);
        tasks.put("daily", Console::onDailyStreak);
        tasks.put("delete_fishery_user", Console::onDeleteFisheryUser);
        tasks.put("fishery_vc", Console::onFisheryVC);
        tasks.put("server", Console::onServer);
        tasks.put("leave_server", Console::onLeaveServer);
        tasks.put("user_find", Console::onUserFind);
        tasks.put("user", Console::onUser);
        tasks.put("servers", Console::onServers);
        tasks.put("servers_mutual", Console::onServersMutual);
        tasks.put("patreon", Console::onPatreon);
        tasks.put("patreon_guild", Console::onPatreonGuild);
        tasks.put("internet", Console::onInternetConnection);
        tasks.put("send_user", Console::onSendUser);
        tasks.put("send_channel", Console::onSendChannel);
    }

    private static void onBotLogsCleanUp(String[] args) {
        BotLogsCleanUp.execute();
    }

    private static void onFisheryCopy(String[] args) {
        long oldGuildId = Long.parseLong(args[1]);
        long newGuildId = Long.parseLong(args[2]);
        MainLogger.get().info("Copying fishery data from {} to {}...", oldGuildId, newGuildId);
        FisheryUserManager.copyGuildData(oldGuildId, newGuildId);
        MainLogger.get().info("Copy process completed!");
    }

    private static void onFeatureLogging(String[] args) {
        int daysSubstract = 1;
        if (args.length > 1) {
            daysSubstract = Integer.parseInt(args[1]);
        }
        FeatureLogger.persist(LocalDate.now().minusDays(daysSubstract));
    }

    private static void onTxt2ImgBan(String[] args) {
        long userId = Long.parseLong(args[1]);
        long durationMinutes = MentionUtil.getTimeMinutes(args[2]).getValue();
        String reason = collectArgs(args, 3).replace("\\n", "\n");

        if (durationMinutes > 0 && Program.isMainCluster() && Program.publicInstance()) {
            Instant bannedUntil = Instant.now().plus(Duration.ofMinutes(durationMinutes));
            try (UserEntity user = HibernateManager.findUserEntity(userId)) {
                Txt2ImgEntity txt2img = user.getTxt2img();

                txt2img.beginTransaction();
                txt2img.setBannedUntil(bannedUntil);
                txt2img.setBannedNumber(txt2img.getBannedNumber() + 1);
                txt2img.setBanReason(reason);
                txt2img.commitTransaction();
            }

            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setDescription("⚠\uFE0F Due to rule violations, you have been banned from the txt2img commands until <t:" + bannedUntil.getEpochSecond() + ":f>!")
                    .addField("Reason", reason.isEmpty() ? "Unspecified" : reason, false);
            JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .queue();

            MainLogger.get().info("{} has been banned from txt2img for {} minutes for the reason: {}", userId, durationMinutes, reason);
        }
    }

    private static void onCasinoLogs(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        CasinoLogCache.get(serverId, userId).forEach(e -> System.out.println(e.toString()));
    }

    private static void onTicketsAutoClose(String[] args) {
        TicketsAutoClose.execute();
    }

    private static void onSlashUpdate(String[] args) {
        MainLogger.get().info("Pushing new slash commands");
        JDA jda = ShardManager.getAnyJDA().get();
        SlashCommandManager.sendCommandUpdate(jda, true);
    }

    private static void onUpdateLawlietSupportCommands(String[] args) {
        ShardManager.getLocalGuildById(AssetIds.SUPPORT_SERVER_ID).ifPresent(guild -> {
            guild.updateCommands()
                    .addCommands(SupportTemplates.generateSupportContextCommands())
                    .queue(commands -> MainLogger.get().info("Successfully sent {} support commands", commands.size()));
        });
    }

    private static void onGdpr(String[] args) throws SQLException, InterruptedException {
        if (Program.isMainCluster() && Program.publicInstance()) {
            long userId = Long.parseLong(args[1]);
            String userTag = args[2];

            JSONObject mySqlJson = GDPR.collectMySQLData(userId, userTag);
            MainLogger.get().info("GDPR MySQL for {}:\n{}", userId, mySqlJson.toString());
            JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), AssetIds.OWNER_USER_ID)
                    .flatMap(messageChannel -> messageChannel.sendFiles(FileUpload.fromData(mySqlJson.toString().getBytes(StandardCharsets.UTF_8), "MySQL.json")))
                    .queue();

            JSONObject redisJson = GDPR.collectRedisData(userId);
            MainLogger.get().info("GDPR Redis for {}:\n{}", userId, redisJson.toString());
            JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), AssetIds.OWNER_USER_ID)
                    .flatMap(messageChannel -> messageChannel.sendFiles(FileUpload.fromData(redisJson.toString().getBytes(StandardCharsets.UTF_8), "Redis.json")))
                    .queue();
        }
    }

    private static void onDevVotesResults(String[] args) throws InterruptedException {
        MainLogger.get().info("Processing development votes results reminders");
        DevelopmentVotesReminder.executeResultsNotification();
    }

    private static void onDevVotes(String[] args) throws InterruptedException {
        MainLogger.get().info("Processing development votes reminders");
        DevelopmentVotesReminder.executeVoteNotification();
    }

    private static void onMuteRefresh(String[] args) {
        MuteRefresh.execute();
    }

    private static void onCleanGuilds(String[] args) {
        try {
            CleanGuilds.execute();
        } catch (InterruptedException e) {
            //ignore
        }
    }

    private static void onAlertsReset(String[] args) {
        AlertScheduler.reset();
        MainLogger.get().info("Alerts reset completed");
    }

    private static void onReminderDaily(String[] args) {
        MainLogger.get().info("Processing daily reminders");
        ReminderDaily.execute();
    }

    private static void onActionsServers(String[] args) {
        int limit = 20;
        if (args.length > 1) {
            limit = Integer.parseInt(args[1]);
        }

        List<Pair<Long, Integer>> guildActionCounts = RestLogger.countGuilds(limit);
        for (Pair<Long, Integer> guildActionCount : guildActionCounts) {
            MainLogger.get().info("{}: {} requests", guildActionCount.getKey(), guildActionCount.getValue());
        }
    }

    private static void onActions(String[] args) {
        MainLogger.get().info("Recent actions in cluster {}: {} requests", Program.getClusterId(), RestLogger.count());
    }

    private static void onStuck(String[] args) {
        MainLogger.get().info("Cluster {} command stuck counter: {}", Program.getClusterId(), CommandContainer.getCommandStuckCounter());
    }

    private static void onClearRoutes(String[] args) {
        RequestRouteLogger.clear();
        MainLogger.get().info("Routes cleared");
    }

    private static void onRoutes(String[] args) {
        int limit = 999;
        if (args.length > 1) {
            limit = Integer.parseInt(args[1]);
        }

        RequestRouteLogger.getRoutes()
                .stream()
                .limit(limit)
                .forEach(entry -> MainLogger.get().info("\"{}\": {} requests", entry.getRoute(), entry.getRequests()));
    }

    private static void onPatreonFetch(String[] args) {
        PatreonCache.getInstance().requestUpdate();
    }

    private static void onSurvey(String[] args) {
        MainLogger.get().info("Processing survey results");
        FisherySurveyResults.processCurrentResults();
    }

    private static void onRepair(String[] args) {
        int minutes = Integer.parseInt(args[1]);
        ShardManager.getConnectedLocalJDAs().forEach(jda -> MainRepair.start(jda, minutes));
        MainLogger.get().info("Repairing cluster {} with {} minutes", Program.getClusterId(), minutes);
    }

    private static void onSendChannel(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long channelId = Long.parseLong(args[2]);
        String text = collectArgs(args, 3).replace("\\n", "\n");

        ShardManager.getLocalGuildById(serverId)
                .map(guild -> guild.getTextChannelById(channelId))
                .ifPresent(channel -> {
                    MainLogger.get().info("#{}: {}", channel.getName(), text);
                    channel.sendMessage(text).queue();
                });
    }

    private static void onSendUser(String[] args) {
        long userId = Long.parseLong(args[1]);
        String text = collectArgs(args, 2).replace("\\n", "\n");
        ShardManager.fetchUserById(userId)
                .exceptionally(ExceptionLogger.get())
                .thenAccept(user -> {
                    MainLogger.get().info("@{}: {}", user.getAsTag(), text);
                    JDAUtil.openPrivateChannel(user)
                            .flatMap(messageChannel -> messageChannel.sendMessage(text))
                            .queue();
                });
    }

    private static void onInternetConnection(String[] args) {
        MainLogger.get().info("Internet connection (Cluster {}): {}", Program.getClusterId(), InternetUtil.checkConnection());
    }

    private static void onPatreon(String[] args) {
        long userId = Long.parseLong(args[1]);
        MainLogger.get().info("Patreon stats of user {}: {}", userId, PatreonCache.getInstance().hasPremium(userId, false));
    }

    private static void onPatreonGuild(String[] args) {
        long guildId = Long.parseLong(args[1]);
        MainLogger.get().info("{} unlocked: {}", guildId, PatreonCache.getInstance().isUnlocked(guildId));
    }

    private static void onServersMutual(String[] args) {
        User user = ShardManager.fetchUserById(AssetIds.OWNER_USER_ID).join();
        ShardManager
                .getLocalMutualGuilds(user)
                .stream()
                .limit(50)
                .forEach(Console::printGuild);
    }

    private static void onServers(String[] args) {
        ArrayList<Guild> guilds = new ArrayList<>(ShardManager.getLocalGuilds());
        int min = args.length > 1 ? Integer.parseInt(args[1]) : 0;

        guilds.stream()
                .filter(g -> g.getMemberCount() >= min)
                .limit(50)
                .forEach(Console::printGuild);
    }

    private static void printGuild(Guild guild) {
        MemberCacheController.getInstance().loadMembersFull(guild).join();
        int bots = (int) guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();
        MainLogger.get().info(
                "Name: {}; ID: {}; Shard: {}; Cluster: {}; Members: {}; Bots {}; Premium {}",
                guild.getName(),
                guild.getId(),
                guild.getJDA().getShardInfo().getShardId(),
                Program.getClusterId(),
                guild.getMemberCount() - bots,
                bots,
                PatreonCache.getInstance().isUnlocked(guild.getIdLong())
        );
    }

    private static void onUserFind(String[] args) {
        String username = collectArgs(args, 1);
        AtomicInteger n = new AtomicInteger();
        for (int i = ShardManager.getShardIntervalMin(); i <= ShardManager.getShardIntervalMax(); i++) {
            ShardManager.getJDA(i).map(JDA::getUsers).ifPresent(users -> {
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

    private static void onUser(String[] args) {
        long userId = Long.parseLong(args[1]);
        ShardManager.fetchUserById(userId)
                .exceptionally(ExceptionLogger.get())
                .thenAccept(user -> MainLogger.get().info(
                        "{} => {} (Premium: {}; Old: {})",
                        user.getId(),
                        user.getAsTag(),
                        PatreonCache.getInstance().hasPremium(user.getIdLong(), false),
                        PatreonCache.getInstance().hasPremium(user.getIdLong(), true)
                ));
    }

    private static void onFisheryVC(String[] args) {
        long serverId = Long.parseLong(args[1]);
        ShardManager.getLocalGuildById(serverId).ifPresent(guild -> {
            HashSet<Member> members = new HashSet<>();
            guild.getVoiceChannels().forEach(vc -> members.addAll(Fishery.getValidVoiceMembers(vc)));

            String title = String.format("### VALID VC MEMBERS OF %s ###", guild.getName());
            System.out.println(title);
            members.forEach(member -> System.out.println(member.getUser().getAsTag()));
            System.out.println("-".repeat(title.length()));
        });
    }

    private static void onServer(String[] args) {
        long serverId = Long.parseLong(args[1]);
        ShardManager.getLocalGuildById(serverId).ifPresent(guild ->
                MainLogger.get().info("{} | Members: {} | Owner: {} | Shard {} | Premium: {}", guild.getName(),
                        guild.getMemberCount(), guild.getOwner().getUser().getAsTag(),
                        guild.getJDA().getShardInfo().getShardId(), PatreonCache.getInstance().isUnlocked(serverId)
                )
        );
    }

    private static void onLeaveServer(String[] args) {
        long serverId = Long.parseLong(args[1]);
        ShardManager.getLocalGuildById(serverId).ifPresent(server -> {
            server.leave().queue();
            MainLogger.get().info("Left server: {}", server.getName());
        });
    }

    private static void onDeleteFisheryUser(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);

        ShardManager.getLocalGuildById(serverId).ifPresent(server -> {
            FisheryUserManager.getGuildData(serverId).getMemberData(userId).remove();
            MainLogger.get().info("Fishery user {} from server {} removed", userId, serverId);
        });
    }

    private static void onDailyStreak(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getLocalGuildById(serverId).ifPresent(server -> {
            FisheryUserManager.getGuildData(serverId).getMemberData(userId).setDailyStreak(value);
            MainLogger.get().info("Changed daily streak value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private static void onCoins(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getLocalGuildById(serverId).ifPresent(server -> {
            FisheryUserManager.getGuildData(serverId).getMemberData(userId).setCoinsRaw(value);
            MainLogger.get().info("Changed coin value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private static void onFish(String[] args) {
        long serverId = Long.parseLong(args[1]);
        long userId = Long.parseLong(args[2]);
        long value = Long.parseLong(args[3]);

        ShardManager.getLocalGuildById(serverId).ifPresent(server -> {
            FisheryUserManager.getGuildData(serverId).getMemberData(userId).setFish(value);
            MainLogger.get().info("Changed fish value (server: {}; user: {}) to {}", serverId, userId, value);
        });
    }

    private static void onUnban(String[] args) {
        long userId = Long.parseLong(args[1]);
        try (UserEntity userEntity = HibernateManager.findUserEntity(userId)) {
            userEntity.beginTransaction();
            userEntity.setBanReason(null);
            userEntity.commitTransaction();
        }

        MainLogger.get().info("User {} unbanned", userId);
    }

    private static void onBan(String[] args) {
        long userId = Long.parseLong(args[1]);
        String reason = collectArgs(args, 2).replace("\\n", "\n");

        try (UserEntity userEntity = HibernateManager.findUserEntity(userId)) {
            userEntity.beginTransaction();
            userEntity.setBanReason(reason);
            userEntity.commitTransaction();
        }

        if (Program.isMainCluster()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setDescription("⚠\uFE0F You have been permanently banned from interacting with Lawliet!")
                    .addField("Reason", reason.isEmpty() ? "Unspecified" : reason, false);
            JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .queue();
        }

        MainLogger.get().info("User {} banned for reason \"{}\"", userId, reason);
    }

    private static void onThreadsStack(String[] args) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                Exception e = ExceptionUtil.generateForStack(t);
                MainLogger.get().error("\n--- {} - {} ---", Program.getClusterId(), t.getName(), e);
            }
        }
    }

    private static void onThreadsInterrupt(String[] args) {
        int stopped = 0;

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                t.interrupt();
                stopped++;
            }
        }

        MainLogger.get().info("{} thread/s interrupted in cluster {}", stopped, Program.getClusterId());
    }

    private static void onThreads(String[] args) {
        StringBuilder sb = new StringBuilder();

        int counter = 0;
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (args.length < 2 || t.getName().matches(args[1])) {
                sb.append(t.getName()).append(", ");
                counter++;
            }
        }

        String str = sb.toString();
        if (str.length() >= 2) str = str.substring(0, str.length() - 2);

        MainLogger.get().info("\n--- THREADS FOR CLUSTER {} ({}) ---\n{}\n", Program.getClusterId(), counter, str);
    }

    private static void onMySQLConnect(String[] args) {
        try {
            MySQLManager.connect();
        } catch (SQLException e) {
            MainLogger.get().error("Exception", e);
        }
    }

    private static void onReconnect(String[] args) {
        int shardId = Integer.parseInt(args[1]);
        ShardManager.reconnectShard(shardId);
    }

    private static void onShards(String[] args) {
        MainLogger.get().info("Cluster: {} - Shards: {} / {}", Program.getClusterId(), ShardManager.getConnectedLocalJDAs().size(), ShardManager.getLocalShards());
        for (int i = ShardManager.getShardIntervalMin(); i <= ShardManager.getShardIntervalMax(); i++) {
            if (!ShardManager.jdaIsConnected(i)) {
                MainLogger.get().info("Shard {} is unavailable!", i);
            }
        }
    }

    private static void onUptime(String[] args) {
        MainLogger.get().info("Uptime cluster {}: {}", Program.getClusterId(), TimeUtil.getRemainingTimeString(Language.EN.getLocale(), Program.getStartTime(), Instant.now(), false));
    }

    private static void onStats(String[] args) {
        MainLogger.get().info(getStats());
    }

    private static void onMemory(String[] args) {
        MainLogger.get().info(getMemory());
    }

    private static void onQuit(String[] args) {
        MainLogger.get().info("EXIT - Stopping cluster {}", Program.getClusterId());
        System.exit(0);
    }

    private static void onHelp(String[] args) {
        tasks.keySet().stream()
                .filter(key -> !key.equals("help"))
                .sorted()
                .forEach(key -> System.out.println("- " + key));
    }

    private static void manageConsole() {
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

    public static void processInput(String input) {
        String[] args = input.split(" ");
        ConsoleTask task = tasks.get(args[0]);
        if (task != null) {
            GlobalThreadPool.submit(() -> {
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

    private static String collectArgs(String[] args, int firstIndex) {
        StringBuilder argsString = new StringBuilder();
        for (int i = firstIndex; i < args.length; i++) {
            argsString.append(" ").append(args[i]);
        }
        return argsString.toString().trim();
    }

    public static String getMemory() {
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

    public static String getStats() {
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
                .append(CommandContainer.getListenerSize())
                .append("\n");

        // running commands
        sb.append("Running Commands: ")
                .append(RunningCheckerManager.getRunningCommandsMap().size())
                .append("\n");

        sb.append("-".repeat(header.length()))
                .append("\n");
        return sb.toString();
    }


    public interface ConsoleTask {

        void process(String[] args) throws Throwable;

    }

}