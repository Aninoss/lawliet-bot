package core;

import core.utils.StringUtil;
import events.discordevents.DiscordEventManager;
import events.scheduleevents.ScheduleEventManager;
import modules.BumpReminder;
import modules.repair.AutoChannelRepair;
import modules.repair.AutoRolesRepair;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.tracker.DBTracker;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.webcomserver.WebComServer;

public class DiscordConnector {

    private static final DiscordConnector ourInstance = new DiscordConnector();
    public static DiscordConnector getInstance() {
        return ourInstance;
    }
    private DiscordConnector() {}

    private final static Logger LOGGER = LoggerFactory.getLogger(DiscordConnector.class);
    private final Intent[] TURNED_OFF_INTENTS = new Intent[] {
            //Intent.GUILD_PRESENCES
    };

    private final DiscordEventManager discordEventManager = new DiscordEventManager();
    private boolean connected = false;

    public void connect() {
        if (connected) return;
        connected = true;

        LOGGER.info("Bot is logging in...");

        DiscordApiBuilder apiBuilder = new DiscordApiBuilder()
                .setToken(SecretManager.getString(Bot.isProductionMode() ? "bot.token" : "bot.token.debugger"))
                .setGlobalRatelimiter(new CustomLocalRatelimiter(1, 21_000_000))
                .setAllIntentsExcept(TURNED_OFF_INTENTS)
                .setWaitForUsersOnStartup(true)
                .setRecommendedTotalShards()
                .join();

        int totalShards = apiBuilder.getTotalShards();
        DiscordApiCollection.getInstance().init(totalShards);

        apiBuilder.loginAllShards()
                .forEach(shardFuture -> {
                            if (shardFuture.thenAccept(this::onApiJoin)
                                    .isCompletedExceptionally()) {
                                LOGGER.error("EXIT - Error while connecting to the Discord servers!");
                                System.exit(-1);
                            }
                        }
                );
    }

    public void reconnectApi(int shardId) {
        LOGGER.info("Shard {} is getting reconnected...", shardId);

        try {
            DiscordApiBuilder apiBuilder = new DiscordApiBuilder()
                    .setToken(SecretManager.getString(Bot.isProductionMode() ? "bot.token" : "bot.token.debugger"))
                    .setGlobalRatelimiter(new CustomLocalRatelimiter(1, 21_000_000))
                    .setAllIntentsExcept(TURNED_OFF_INTENTS)
                    .setWaitForUsersOnStartup(true)
                    .setTotalShards(DiscordApiCollection.getInstance().size())
                    .setCurrentShard(shardId);

            DiscordApi api = apiBuilder.login().get();
            onApiJoin(api);
        } catch (Throwable e) {
            LOGGER.error("EXIT - Exception when reconnecting shard {}", shardId, e);
            System.exit(-1);
        }
    }

    public void onApiJoin(DiscordApi api) {
        api.setAutomaticMessageCacheCleanupEnabled(true);
        api.setMessageCacheSize(30, 30 * 60);

        DiscordApiCollection.getInstance().insertApi(api);
        startRepairProcesses(api);
        LOGGER.info("Shard {} connection established", api.getCurrentShard());

        if (DiscordApiCollection.getInstance().allShardsConnected()) {
            if (!DiscordApiCollection.getInstance().isStarted()) {
                onConnectionCompleted();
            } else {
                updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
            }
        }

        discordEventManager.registerApi(api);
        api.addReconnectListener(event -> new CustomThread(() -> onSessionResume(event.getApi()), "reconnect").start());
    }

    private void startRepairProcesses(DiscordApi api) {
        new AutoChannelRepair(api).start();
        new AutoRolesRepair(api).start();
    }

    private void onConnectionCompleted() {
        updateActivity();
        DBFishery.getInstance().cleanUp();
        DBFishery.getInstance().startVCObserver();
        new WebComServer(15744);
        new ScheduleEventManager().start();
        DBTracker.getInstance().start();
        if (Bot.isProductionMode()) BumpReminder.getInstance().start();

        DiscordApiCollection.getInstance().setStarted();
        LOGGER.info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
    }

    public void updateActivity() {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        int serverTotalSize = apiCollection.getServerTotalSize();
        for (DiscordApi api : apiCollection.getApis()) {
            updateActivity(api, serverTotalSize);
        }
    }

    public void updateActivity(DiscordApi api, int serverNumber) {
        api.updateStatus(UserStatus.ONLINE);
        api.updateActivity(ActivityType.WATCHING, "L.help | " + StringUtil.numToString(serverNumber) + " | www.lawlietbot.xyz");
    }

    private void onSessionResume(DiscordApi api) {
        LOGGER.debug("Connection has been reestablished!");
        updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
    }

}