package core;

import core.schedule.MainScheduler;
import core.utils.StringUtil;
import events.discordevents.DiscordEventManager;
import events.scheduleevents.ScheduleEventManager;
import modules.BumpReminder;
import modules.FisheryVCObserver;
import modules.repair.MainRepair;
import modules.schedulers.GiveawayScheduler;
import modules.schedulers.ReminderScheduler;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.survey.DBSurvey;
import mysql.modules.tracker.DBTracker;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SyncManager;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class DiscordConnector {

    private static final DiscordConnector ourInstance = new DiscordConnector();

    public static DiscordConnector getInstance() {
        return ourInstance;
    }

    private DiscordConnector() {
        DiscordApiManager.getInstance().addShardDisconnectConsumer(this::reconnectApi);
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(DiscordConnector.class);
    private final DiscordEventManager discordEventManager = new DiscordEventManager();
    private boolean started = false;

    public void connect(int shardMin, int shardMax, int totalShards) {
        if (started) return;
        started = true;

        DBFishery.getInstance().cleanUp();
        FisheryVCObserver.getInstance().start();

        LOGGER.info("Bot is logging in...");
        DiscordApiBuilder apiBuilder = createBuilder()
                .setTotalShards(totalShards);

        DiscordApiManager.getInstance().init(shardMin, shardMax, totalShards);

        apiBuilder.loginShards(shard -> shard >= shardMin && shard <= shardMax)
                .forEach(apiFuture -> apiFuture.thenAccept(this::onApiJoin)
                        .exceptionally(e -> {
                            LOGGER.error("EXIT - Error while connecting to the Discord servers!", e);
                            System.exit(-1);
                            return null;
                        })
                );
    }

    public void reconnectApi(int shardId) {
        LOGGER.info("Shard {} is getting reconnected...", shardId);

        createBuilder().setTotalShards(DiscordApiManager.getInstance().getTotalShards())
                .setCurrentShard(shardId)
                .login()
                .thenAccept(this::onApiJoin)
                .exceptionally(e -> {
                    LOGGER.error("Exception when reconnecting shard {}", shardId, e);
                    MainScheduler.getInstance().schedule(5, ChronoUnit.SECONDS, "shard_reconnect", () -> reconnectApi(shardId));
                    return null;
                });
    }

    private DiscordApiBuilder createBuilder() {
        return new DiscordApiBuilder()
                .setToken(SecretManager.getString(Bot.isProductionMode() ? "bot.token" : "bot.token.debugger"))
                .setGlobalRatelimiter(new CustomLocalRatelimiter(1, 21_000_000))
                .setAllIntentsExcept(Intent.DIRECT_MESSAGE_TYPING, Intent.GUILD_MESSAGE_TYPING)
                .setWaitForUsersOnStartup(true)
                .setShutdownHookRegistrationEnabled(false);
    }

    public void onApiJoin(DiscordApi api) {
        api.setAutomaticMessageCacheCleanupEnabled(true);
        api.setMessageCacheSize(0, 0);

        DiscordApiManager.getInstance().addApi(api);
        LOGGER.info("Shard {} connection established", api.getCurrentShard());

        if (DiscordApiManager.getInstance().isEverythingConnected() && !DiscordApiManager.getInstance().isFullyConnected()) {
            onConnectionCompleted();
        }

        updateActivity(api);
        MainRepair.start(api, 1);
        discordEventManager.registerApi(api);
        api.addReconnectListener(event -> onSessionResume(event.getApi()));
    }

    private void onConnectionCompleted() {
        DBSurvey.getInstance().getCurrentSurvey();
        new ScheduleEventManager().start();
        DBTracker.getInstance().start();
        if (Bot.isProductionMode() && Bot.isPublicVersion()) BumpReminder.getInstance().start();
        ReminderScheduler.getInstance().start();
        GiveawayScheduler.getInstance().start();

        DiscordApiManager.getInstance().start();
        SyncManager.getInstance().setFullyConnected();
        LOGGER.info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
    }

    public void updateActivity(DiscordApi api) {
        api.updateStatus(UserStatus.ONLINE);
        Optional<Long> serverSizeOpt = DiscordApiManager.getInstance().getGlobalServerSize();
        if (serverSizeOpt.isPresent())
            api.updateActivity(ActivityType.WATCHING, "L.help | " + StringUtil.numToString(serverSizeOpt.get()) + " | www.lawlietbot.xyz");
        else
            api.updateActivity(ActivityType.WATCHING, "L.help | www.lawlietbot.xyz");
    }

    private void onSessionResume(DiscordApi api) {
        LOGGER.debug("Connection has been reestablished!");
        updateActivity(api);
    }

    public boolean isStarted() {
        return started;
    }

}