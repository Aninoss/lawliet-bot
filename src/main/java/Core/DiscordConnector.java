package Core;

import Constants.Settings;
import Core.Utils.StringUtil;
import Events.DiscordEvents.DiscordEventManager;
import Events.ScheduleEvents.ScheduleEventManager;
import MySQL.DBMain;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.Tracker.DBTracker;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

public class DiscordConnector {

    private static final DiscordConnector ourInstance = new DiscordConnector();

    public static DiscordConnector getInstance() {
        return ourInstance;
    }

    private DiscordConnector() {
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(DiscordConnector.class);

    private final DiscordEventManager discordEventManager = new DiscordEventManager();
    private boolean connected = false;

    public void connect() {
        if (connected) return;
        connected = true;

        LOGGER.info("Bot is logging in...");

        DiscordApiBuilder apiBuilder = new DiscordApiBuilder()
                .setToken(SecretManager.getString(Bot.isProductionMode() ? "bot.token" : "bot.token.debugger"))
                .setRecommendedTotalShards().join();

        int totalShards = apiBuilder.getTotalShards();
        DiscordApiCollection.getInstance().init(totalShards);

        apiBuilder.loginAllShards()
                .forEach(shardFuture -> {
                            if (shardFuture.thenAccept(api -> onApiJoin(api, true))
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
                    .setTotalShards(DiscordApiCollection.getInstance().size())
                    .setCurrentShard(shardId);

            DiscordApi api = apiBuilder.login().get();
            onApiJoin(api, false);
        } catch (Throwable e) {
            LOGGER.error("EXIT - Exception when reconnecting shard {}", shardId, e);
            System.exit(-1);
        }
    }

    public void onApiJoin(DiscordApi api, boolean startup) {
        api.setAutomaticMessageCacheCleanupEnabled(true);
        api.setMessageCacheSize(30, 30 * 60);

        DiscordApiCollection.getInstance().insertApi(api);
        DBAutoChannel.getInstance().synchronize(api);

        LOGGER.info("Shard {} connection established", api.getCurrentShard());

        if (DiscordApiCollection.getInstance().allShardsConnected()) {
            if (startup) {
                updateActivity();
                DBFishery.getInstance().cleanUp();
                new WebComServer(15744);
                DBFishery.getInstance().startVCObserver();
                new ScheduleEventManager().start();
                DBTracker.getInstance().start();

                LOGGER.info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
            } else {
                updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
            }
        }

        discordEventManager.registerApi(api);
        api.addReconnectListener(event -> new CustomThread(() -> onSessionResume(event.getApi()), "reconnect").start());
    }

    public void updateActivity() {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        int serverTotalSize = apiCollection.getServerTotalSize();
        for (DiscordApi api : apiCollection.getApis()) {
            updateActivity(api, serverTotalSize);
        }
    }

    public void updateActivity(DiscordApi api, int serverNumber) {
        Calendar calendar = Calendar.getInstance();
        boolean isRestartPending = calendar.get(Calendar.HOUR_OF_DAY) == Settings.UPDATE_HOUR &&
                Bot.hasUpdate();

        if (!isRestartPending) {
            if (DBMain.getInstance().checkConnection()) {
                api.updateStatus(UserStatus.ONLINE);
                api.updateActivity(ActivityType.WATCHING, "L.help | " + StringUtil.numToString(serverNumber) + " | www.lawlietbot.xyz");
            } else {
                api.updateStatus(UserStatus.DO_NOT_DISTURB);
                api.updateActivity(ActivityType.WATCHING, "ERROR - DATABASE DOWN");
            }
        } else {
            api.updateStatus(UserStatus.DO_NOT_DISTURB);
            api.updateActivity(ActivityType.WATCHING, "BOT RESTARTS SOON");
        }
    }


    private void onSessionResume(DiscordApi api) {
        LOGGER.debug("Connection has been reestablished!");
        updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
    }

}