package core;

import commands.SlashCommandManager;
import core.featurelogger.FeatureLogger;
import core.utils.StringUtil;
import events.discordevents.DiscordEventAdapter;
import events.scheduleevents.ScheduleEventManager;
import events.sync.SendEvent;
import modules.BumpReminder;
import modules.repair.MainRepair;
import modules.schedulers.*;
import mysql.DBDataLoadAll;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.GuildEntity;
import mysql.hibernate.entity.StickyRolesActiveRoleEntity;
import mysql.hibernate.template.HibernateEntityInterface;
import mysql.modules.stickyroles.DBStickyRoles;
import mysql.modules.stickyroles.StickyRolesData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ConcurrentSessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.dv8tion.jda.internal.utils.IOUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

public class DiscordConnector {

    private static boolean started = false;
    private static final ConcurrentSessionController concurrentSessionController = new ConcurrentSessionController();

    private static final JDABuilder jdaBuilder = JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
            .setSessionController(concurrentSessionController)
            .setMemberCachePolicy(MemberCacheController.getInstance())
            .setChunkingFilter(ChunkingFilterController.getInstance())
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.MESSAGE_CONTENT)
            .enableCache(CacheFlag.ACTIVITY)
            .disableCache(CacheFlag.ROLE_TAGS)
            .setHttpClient(IOUtil.newHttpClientBuilder().addInterceptor(new CustomInterceptor()).build())
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
        if (Program.publicVersion()) {
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

        transferSqlToHibernate();

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

    public static void onJDAJoin(JDA jda) {
        if (!Program.publicVersion()) {
            if (jda.getGuilds().size() - 2 > Integer.parseInt(System.getenv("MAX_SERVERS"))) {
                MainLogger.get().warn("Total server limit reached, refusing to boot up");
                return;
            }

            long subId = Long.parseLong(System.getenv("SUB_ID"));
            try {
                if (subId != -1 && !SendEvent.sendSubscriptionActive(subId).get(5, TimeUnit.SECONDS)) {
                    MainLogger.get().warn("Subscription not active anymore, refusing to boot up");
                    return;
                } else {
                    MainLogger.get().info("Subscription check passed");
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                MainLogger.get().error("Subscription retrieval error", e);
                return;
            }
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

    private synchronized static void allConnectionsCompleted() {
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
        FeatureLogger.start();
        MainLogger.get().info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
    }

    private static <T extends HibernateEntityInterface> void transferSqlToHibernate() {
        transferSqlToHibernate(
                "StickyRolesRoles",
                GuildEntity::getStickyRoles,
                stickyRolesEntity -> !stickyRolesEntity.getRoleIds().isEmpty() || !stickyRolesEntity.getActiveRoles().isEmpty(),
                stickyRolesEntity -> {
                    StickyRolesData stickyRolesSql = DBStickyRoles.getInstance().retrieve(stickyRolesEntity.getGuildId());
                    Set<StickyRolesActiveRoleEntity> activeRoles = stickyRolesSql.getActions().stream()
                            .map(action -> new StickyRolesActiveRoleEntity(action.getMemberId(), action.getRoleId()))
                            .collect(Collectors.toSet());

                    stickyRolesEntity.setRoleIds(stickyRolesSql.getRoleIds());
                    stickyRolesEntity.setActiveRoles(activeRoles);
                }
        );
    }

    private static <T extends HibernateEntityInterface> void transferSqlToHibernate(
            String sqlTableName,
            Function<GuildEntity, T> entityFunction,
            Function<T, Boolean> isUsedFunction,
            Consumer<T> updateEntityValuesConsumer
    ) {
        if (!Program.publicVersion()) {
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

            for (long guildId : guildIdList) {
                try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId)) {
                    T entity = entityFunction.apply(guildEntity);
                    if (isUsedFunction.apply(entity)) {
                        continue;
                    }

                    entity.beginTransaction();
                    updateEntityValuesConsumer.accept(entity);
                    entity.commitTransaction();
                    updates++;
                }
            }
            if (!guildIdList.isEmpty()) {
                guildIdOffset = guildIdList.get(guildIdList.size() - 1);
            }
        } while (guildIdList.size() == limit);

        MainLogger.get().info("Completed with {} updates!", updates);
    }

}