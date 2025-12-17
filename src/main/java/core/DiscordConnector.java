package core;

import commands.SlashCommandManager;
import core.featurelogger.FeatureLogger;
import core.schedule.MainScheduler;
import core.utils.StringUtil;
import events.discordevents.DiscordEventAdapter;
import events.scheduleevents.ScheduleEventManager;
import events.sync.SendEvent;
import modules.BumpReminder;
import modules.repair.MainRepair;
import modules.schedulers.*;
import mysql.DBDataLoadAll;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.template.HibernateEntityInterface;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestConfig;
import net.dv8tion.jda.api.utils.ConcurrentSessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNullElse;

public class DiscordConnector {

    private static boolean started = false;
    private static final ConcurrentSessionController concurrentSessionController = new ConcurrentSessionController();

    private static final JDABuilder jdaBuilder = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
            .setSessionController(concurrentSessionController)
            .setMemberCachePolicy(MemberCacheController.getInstance())
            .setChunkingFilter(ChunkingFilterController.getInstance())
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.MESSAGE_CONTENT)
            .enableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE)
            .disableCache(CacheFlag.ROLE_TAGS)
            .setRequestTimeoutRetry(false)
            .setRestConfig(new RestConfig()
                    .setBaseUrl(System.getenv("NIRN_PROXY_URL") + "/api/v" + JDAInfo.DISCORD_REST_VERSION + "/"))
            .addEventListeners(new DiscordEventAdapter());

    static {
        concurrentSessionController.setConcurrency(Integer.parseInt(System.getenv("CONCURRENCY")));
        ShardManager.addShardDisconnectConsumer(DiscordConnector::reconnectApi);

        String activityText = getActivityText();
        if (!activityText.isEmpty()) {
            jdaBuilder.setActivity(createActivity(activityText));
        }
    }

    public static Activity createActivity(String activityText) {
        Activity.ActivityType activityType = System.getenv("ACTIVITY_TYPE") != null
                ? Activity.ActivityType.valueOf(System.getenv("ACTIVITY_TYPE"))
                : Activity.ActivityType.CUSTOM_STATUS;

        return Activity.of(activityType, activityText);
    }

    public static String getActivityText() {
        if (Program.publicInstance()) {
            return ShardManager.getGlobalGuildSize()
                    .map(globalGuildSize -> "L.help｜" + StringUtil.numToStringShort(globalGuildSize, Locale.US) + "｜www.lawlietbot.xyz")
                    .orElse("L.help｜www.lawlietbot.xyz");
        } else {
            return requireNonNullElse(System.getenv("ACTIVITY"), "");
        }
    }

    public static void connect(int shardMin, int shardMax, int totalShards) {
        if (started) return;
        started = true;

        MainLogger.get().info("Bot is logging in...");
        ShardManager.init(shardMin, shardMax, totalShards);
        EnumSet<Message.MentionType> deny = EnumSet.of(Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE);
        MessageRequest.setDefaultMentions(EnumSet.complementOf(deny));
        MessageRequest.setDefaultMentionRepliedUser(false);

        new Thread(() -> {
            for (int i = shardMin; i <= shardMax; i++) {
                try {
                    jdaBuilder.useSharding(i, totalShards)
                            .build();
                } catch (InvalidTokenException e) {
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
        } catch (InvalidTokenException e) {
            MainLogger.get().error("EXIT - Invalid token", e);
            System.exit(3);
        }
    }

    public synchronized static void onJDAJoin(JDA jda) {
        if (!checkCustomBotParameters(jda)) {
            return;
        }

        boolean firstConnection = ShardManager.isNothingConnected();
        ShardManager.addJDA(jda);
        MainLogger.get().info("Shard {} connection established", jda.getShardInfo().getShardId());

        if (firstConnection) {
            SlashCommandManager.sendCommandUpdate(jda, false);
        }
        if (ShardManager.isEverythingConnected() && !ShardManager.isReady()) {
            allConnectionsCompleted();
        }
        if (Program.productionMode()) {
            MainRepair.start(jda, 20);
        }
    }

    private static boolean checkCustomBotParameters(JDA jda) {
        if (Program.publicInstance()) {
            return true;
        }

        if (jda.getGuilds().size() - 2 > Integer.parseInt(System.getenv("MAX_SERVERS"))) {
            MainLogger.get().warn("Total server limit reached, refusing to boot up");
            blockBootUp();
            return false;
        }

        long subId = Long.parseLong(System.getenv("SUB_ID"));
        try {
            if (subId != -1 && !SendEvent.sendSubscriptionActive(subId).get(5, TimeUnit.SECONDS)) {
                MainLogger.get().warn("Subscription not active anymore, refusing to boot up");
                blockBootUp();
                return false;
            } else {
                MainLogger.get().info("Subscription check passed");
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            MainLogger.get().error("Subscription retrieval error", e);
            blockBootUp();
            return false;
        }

        return true;
    }

    private static void blockBootUp() {
        ShardManager.blockBootUpCheck();
        MainScheduler.schedule(Duration.ofHours(1), () -> {
            MainLogger.get().info("EXIT - Custom instance not valid anymore");
            System.exit(10);
        });
    }

    private static void allConnectionsCompleted() {
        new ScheduleEventManager().start();
        if (Program.productionMode() && Program.publicInstance()) {
            BumpReminder.start();
        }
        AlertScheduler.start();
        ReminderScheduler.start();
        GiveawayScheduler.start();
        TempBanScheduler.start();
        ServerMuteScheduler.start();
        JailScheduler.start();
        ChannelLockScheduler.start();
        ShardManager.start();
        FeatureLogger.start();
        MainLogger.get().info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
    }

    private static <T extends HibernateEntityInterface> void transferSqlToHibernate(
            String sqlTableName,
            Function<GuildEntity, T> entityFunction,
            Function<T, Boolean> isUsedFunction,
            Consumer<T> updateEntityValuesConsumer
    ) {
        if (!Program.publicInstance()) {
            return;
        }

        MainLogger.get().info("Transferring {} MySQL data to MongoDB...", sqlTableName);
        List<Long> guildIdList;
        int limit = 100;
        long guildIdOffset = 0;
        int updates = 0;
        do {
            guildIdList = new DBDataLoadAll<Long>(sqlTableName, "serverId", " AND serverId > " + guildIdOffset + " ORDER BY serverId LIMIT " + limit)
                    .getList(resultSet -> resultSet.getLong(1));

            try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(DiscordConnector.class)) {
                for (long guildId : guildIdList) {
                    GuildEntity guildEntity = entityManager.find(GuildEntity.class, String.valueOf(guildId));
                    if (guildEntity == null) {
                        continue;
                    }

                    T entity = entityFunction.apply(guildEntity);
                    if (isUsedFunction.apply(entity)) {
                        continue;
                    }

                    entity.beginTransaction();
                    updateEntityValuesConsumer.accept(entity);
                    entity.commitTransaction();
                    updates++;

                    entityManager.clear();
                }
            }
            if (!guildIdList.isEmpty()) {
                guildIdOffset = guildIdList.get(guildIdList.size() - 1);
            }
        } while (guildIdList.size() == limit);

        MainLogger.get().info("Completed with {} updates!", updates);
    }

}