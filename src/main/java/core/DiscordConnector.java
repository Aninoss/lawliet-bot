package core;

import commands.SlashCommandManager;
import constants.AssetIds;
import constants.Language;
import core.utils.StringUtil;
import events.discordevents.DiscordEventAdapter;
import events.scheduleevents.ScheduleEventManager;
import events.sync.SendEvent;
import modules.BumpReminder;
import modules.SupportTemplates;
import modules.repair.MainRepair;
import modules.schedulers.*;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.guild.GuildKickedData;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ConcurrentSessionController;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageRequest;
import net.dv8tion.jda.internal.utils.IOUtil;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
                : Activity.ActivityType.WATCHING;

        return Activity.of(activityType, activityText);
    }

    public static String getActivityText() {
        if (Program.publicVersion()) {
            return ShardManager.getGlobalGuildSize()
                    .map(globalGuildSize -> "L.help | " + StringUtil.numToStringShort(globalGuildSize, Locale.US) + " | www.lawlietbot.xyz")
                    .orElse("L.help | www.lawlietbot.xyz");
        } else {
            return System.getenv("ACTIVITY");
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
            firstConnectionCompleted(jda);
        }
        if (ShardManager.isEverythingConnected() && !ShardManager.isReady()) {
            allConnectionsCompleted();
        }
        if (Program.productionMode()) {
            MainRepair.start(jda, 20);
        }
    }

    private static void firstConnectionCompleted(JDA jda) {
        try {
            List<CommandData> commandDataList = SlashCommandManager.initialize();
            if (Program.productionMode()) {
                jda.retrieveCommands().queue(commands -> {
                    if (commands.isEmpty() || Program.isNewVersion()) {
                        MainLogger.get().info("Pushing new slash commands");
                        jda.updateCommands()
                                .addCommands(commandDataList)
                                .queue(SlashAssociations::registerSlashCommands);
                    } else {
                        MainLogger.get().info("Skipping slash commands because it's not a new version");
                        jda.retrieveCommands().queue(SlashAssociations::registerSlashCommands);
                    }
                });
            } else {
                ShardManager.getLocalGuildById(AssetIds.BETA_SERVER_ID).get()
                        .updateCommands()
                        .addCommands(commandDataList)
                        .addCommands(SupportTemplates.generateSupportContextCommands())
                        .queue(commands -> {
                            SlashAssociations.registerSlashCommands(commands);
                            MainLogger.get().info("Successfully sent {} slash commands", commands.size());
                        });
            }
        } catch (Throwable e) {
            MainLogger.get().error("Exception on slash commands load", e);
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
        MainLogger.get().info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
    }

    private static void transferFisherySqlToHibernate() { //TODO: keep method until migration is complete
        if (!Program.publicVersion()) {
            return;
        }

        MainLogger.get().info("Transferring MySQL data to MongoDB...");
        List<GuildKickedData> guildKickedDataList;
        int limit = 100;
        long guildIdOffset = 0;
        do {
            guildKickedDataList = DBGuild.getInstance().retrieveKickedData(guildIdOffset, limit);
            for (GuildKickedData guildKickedData : guildKickedDataList) {
                try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager()) {
                    GuildData guildData = DBGuild.getInstance().retrieve(guildKickedData.getGuildId());
                    FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(guildKickedData.getGuildId());
                    GuildEntity guildEntity = entityManager.find(GuildEntity.class, String.valueOf(guildData.getGuildId()));
                    if (guildEntity == null) {
                        guildEntity = new GuildEntity(String.valueOf(guildData.getGuildId()));
                        guildEntity.setPrefix(guildData.getPrefix());
                        guildEntity.setLanguage(Language.from(guildData.getLocale()));
                        guildEntity.setRemoveAuthorMessage(guildData.isCommandAuthorMessageRemove());
                        // set property here

                        entityManager.getTransaction().begin();
                        entityManager.persist(guildEntity);
                        entityManager.getTransaction().commit();
                    } else if (guildEntity.getFishery().getRolePriceMin() == null) {
                        entityManager.getTransaction().begin();
                        // set property here
                        entityManager.getTransaction().commit();
                    }
                }
            }
            if (!guildKickedDataList.isEmpty()) {
                guildIdOffset = guildKickedDataList.get(guildKickedDataList.size() - 1).getGuildId();
            }
        } while (guildKickedDataList.size() == limit);
    }

}