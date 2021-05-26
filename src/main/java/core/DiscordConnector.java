package core;

import java.util.EnumSet;
import java.util.concurrent.ForkJoinPool;
import javax.security.auth.login.LoginException;
import com.neovisionaries.ws.client.WebSocketFactory;
import core.utils.StringUtil;
import events.discordevents.DiscordEventAdapter;
import events.scheduleevents.ScheduleEventManager;
import modules.BumpReminder;
import modules.repair.MainRepair;
import modules.schedulers.*;
import mysql.modules.fisheryusers.DBFishery;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.ConcurrentSessionController;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.IOUtil;
import net.dv8tion.jda.internal.utils.config.ThreadingConfig;
import okhttp3.Interceptor;
import websockets.syncserver.SyncManager;

public class DiscordConnector {

    private static final DiscordConnector ourInstance = new DiscordConnector();

    public static DiscordConnector getInstance() {
        return ourInstance;
    }

    private boolean started = false;
    private final CustomLocalRatelimiter customLocalRatelimiter = new CustomLocalRatelimiter(21);
    private final ConcurrentSessionController concurrentSessionController = new ConcurrentSessionController();

    private final Interceptor interceptor = chain -> {
        try {
            customLocalRatelimiter.requestQuota();
        } catch (InterruptedException e) {
            MainLogger.get().error("Interrupted", e);
        }
        return chain.proceed(chain.request());
    };

    private final JDABuilder jdaBuilder = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
            .setSessionController(concurrentSessionController)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setChunkingFilter(ChunkingFilter.ALL)
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
            .enableCache(CacheFlag.ACTIVITY)
            .disableCache(CacheFlag.ROLE_TAGS)
            .setActivity(Activity.watching(getActivityText()))
            .setHttpClient(IOUtil.newHttpClientBuilder().addInterceptor(interceptor).build())
            .setWebsocketFactory(new WebSocketFactory())
            .setRateLimitPool(ThreadingConfig.newScheduler(32, () -> "JDA", "RateLimit", false))
            .setGatewayPool(ThreadingConfig.newScheduler(16, () -> "JDA", "Gateway", true))
            .setCallbackPool(ForkJoinPool.commonPool())
            .setAudioPool(ThreadingConfig.newScheduler(4, () -> "JDA", "Audio", true))
            .setRawEventsEnabled(true)
            .addEventListeners(new DiscordEventAdapter());

    private DiscordConnector() {
        concurrentSessionController.setConcurrency(Integer.parseInt(System.getenv("CONCURRENCY")));
        ShardManager.getInstance().addShardDisconnectConsumer(this::reconnectApi);
    }

    public void connect(int shardMin, int shardMax, int totalShards) {
        if (started) return;
        started = true;

        ShardManager.getInstance().init(shardMin, shardMax, totalShards);
        DBFishery.getInstance().cleanUp();

        MainLogger.get().info("Bot is logging in...");
        EnumSet<Message.MentionType> deny = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE);
        MessageAction.setDefaultMentions(EnumSet.complementOf(deny));

        new Thread(() -> {
            for (int i = shardMin; i <= shardMax; i++) {
                try {
                    jdaBuilder.useSharding(i, totalShards)
                            .build();
                } catch (LoginException e) {
                    MainLogger.get().error("EXIT - Invalid token", e);
                    System.exit(1);
                }
            }
        }, "Shard-Starter").start();
    }

    public void reconnectApi(int shardId) {
        MainLogger.get().info("Shard {} is getting reconnected...", shardId);

        try {
            jdaBuilder.useSharding(shardId, ShardManager.getInstance().getTotalShards())
                    .build();
        } catch (LoginException e) {
            MainLogger.get().error("EXIT - Invalid token", e);
            System.exit(1);
        }
    }

    public void onJDAJoin(JDA jda) {
        ShardManager.getInstance().addJDA(jda);
        MainLogger.get().info("Shard {} connection established", jda.getShardInfo().getShardId());

        if (ShardManager.getInstance().isEverythingConnected() && !ShardManager.getInstance().isReady()) {
            onConnectionCompleted();
        }

        MainRepair.start(jda, 5);
    }

    private void onConnectionCompleted() {
        new ScheduleEventManager().start();
        if (Program.isProductionMode() && Program.isPublicVersion()) BumpReminder.getInstance().start();
        AlertScheduler.getInstance().start();
        ReminderScheduler.getInstance().start();
        GiveawayScheduler.getInstance().start();
        TempBanScheduler.getInstance().start();
        ServerMuteScheduler.getInstance().start();

        ShardManager.getInstance().start();
        SyncManager.getInstance().setFullyConnected();
        MainLogger.get().info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
    }

    public void updateActivity(JDA jda) {
        jda.getPresence().setActivity(Activity.watching(getActivityText()));
    }

    private String getActivityText() {
        return ShardManager.getInstance().getGlobalGuildSize()
                .map(globalGuildSize -> "L.help | " + StringUtil.numToString(globalGuildSize) + " | www.lawlietbot.xyz")
                .orElse("L.help | www.lawlietbot.xyz");
    }

    public boolean isStarted() {
        return started;
    }

}