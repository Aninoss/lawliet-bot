package core;

import java.util.EnumSet;
import javax.security.auth.login.LoginException;
import core.utils.StringUtil;
import events.discordevents.DiscordEventAdapter;
import events.scheduleevents.ScheduleEventManager;
import modules.BumpReminder;
import modules.repair.MainRepair;
import modules.schedulers.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.ConcurrentSessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.IOUtil;
import websockets.syncserver.SyncManager;

public class DiscordConnector {

    private static boolean started = false;
    private static final ConcurrentSessionController concurrentSessionController = new ConcurrentSessionController();

    private static final JDABuilder jdaBuilder = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
            .setSessionController(concurrentSessionController)
            .setMemberCachePolicy(MemberCacheController.getInstance())
            .setChunkingFilter(new ChunkingFilterController())
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING)
            .enableCache(CacheFlag.ACTIVITY)
            .disableCache(CacheFlag.ROLE_TAGS)
            .setActivity(Activity.watching(getActivityText()))
            .setHttpClient(IOUtil.newHttpClientBuilder().addInterceptor(new CustomInterceptor()).build())
            .addEventListeners(new DiscordEventAdapter());

    static {
        concurrentSessionController.setConcurrency(Integer.parseInt(System.getenv("CONCURRENCY")));
        ShardManager.addShardDisconnectConsumer(DiscordConnector::reconnectApi);
    }

    public static void connect(int shardMin, int shardMax, int totalShards) {
        if (started) return;
        started = true;

        MainLogger.get().info("Bot is logging in...");
        ShardManager.init(shardMin, shardMax, totalShards);
        EnumSet<Message.MentionType> deny = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE);
        MessageAction.setDefaultMentions(EnumSet.complementOf(deny));

        new Thread(() -> {
            for (int i = shardMin; i <= shardMax; i++) {
                try {
                    jdaBuilder.useSharding(i, totalShards)
                            .build();
                } catch (LoginException e) {
                    MainLogger.get().error("EXIT - Invalid token", e);
                    System.exit(2);
                }
            }
        }, "Shard-Starter").start();
    }

    public static void reconnectApi(int shardId) {
        MainLogger.get().info("Shard {} is getting reconnected...", shardId);

        try {
            jdaBuilder.useSharding(shardId, ShardManager.getTotalShards())
                    .build();
        } catch (LoginException e) {
            MainLogger.get().error("EXIT - Invalid token", e);
            System.exit(3);
        }
    }

    public static void onJDAJoin(JDA jda) {
        ShardManager.addJDA(jda);
        MainLogger.get().info("Shard {} connection established", jda.getShardInfo().getShardId());

        checkConnectionCompleted();
        MainRepair.start(jda, 5);
    }

    private synchronized static void checkConnectionCompleted() {
        if (ShardManager.isEverythingConnected() && !ShardManager.isReady()) {
            onConnectionCompleted();
        }
    }

    private synchronized static void onConnectionCompleted() {
        new ScheduleEventManager().start();
        if (Program.productionMode() && Program.publicVersion()) {
            BumpReminder.start();
        }
        AlertScheduler.start();
        ReminderScheduler.start();
        GiveawayScheduler.start();
        TempBanScheduler.start();
        ServerMuteScheduler.start();
        JailScheduler.start();
        ShardManager.start();
        SyncManager.setFullyConnected();
        MainLogger.get().info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
    }

    public static void updateActivity(JDA jda) {
        jda.getPresence().setActivity(Activity.watching(getActivityText()));
    }

    private static String getActivityText() {
        return ShardManager.getGlobalGuildSize()
                .map(globalGuildSize -> "L.help | " + StringUtil.numToString(globalGuildSize) + " | www.lawlietbot.xyz")
                .orElse("L.help | www.lawlietbot.xyz");
    }

    public static boolean isStarted() {
        return started;
    }

}