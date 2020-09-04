package Core;

import Constants.Settings;
import Core.Utils.BotUtil;
import Core.Utils.StringUtil;
import Events.DiscordEvents.DiscordEventManager;
import Events.ScheduleEvents.ScheduleEventManager;
import MySQL.DBMain;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.Tracker.DBTracker;
import MySQL.Modules.Version.DBVersion;
import MySQL.Modules.Version.VersionBean;
import MySQL.Modules.Version.VersionBeanSlot;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;

public class Connector {

    private final static Logger LOGGER = LoggerFactory.getLogger(Connector.class);

    public static void main(String[] args) {
        boolean production = args.length >= 1 && args[0].equals("production");
        Bot.setDebug(production);
        Runtime.getRuntime().addShutdownHook(new CustomThread(Bot::onStop, "shutdown_botstop"));

        Console.getInstance().start();

        try {
            FontContainer.getInstance().init();
            DBMain.getInstance().connect();
            cleanAllTempFiles();
            initializeUpdate();
            connect();
        } catch (Throwable e) {
            LOGGER.error("EXIT - Exception in main method", e);
            System.exit(-1);
        }
    }

    private static void cleanAllTempFiles() {
        File[] files = new File("temp").listFiles();
        if (files != null)
            Arrays.stream(files).forEach(file -> {
                if (!file.delete()) {
                    LOGGER.error("Temp file {} could not be removed!", file.getName());
                }
            });
    }

    private static void initializeUpdate() {
        try {
            VersionBean versionBean = DBVersion.getInstance().getBean();

            String currentVersionDB = versionBean.getCurrentVersion().getVersion();
            if (!BotUtil.getCurrentVersion().equals(currentVersionDB))
                versionBean.getSlots().add(new VersionBeanSlot(BotUtil.getCurrentVersion(), Instant.now()));
        } catch (SQLException e) {
            LOGGER.error("EXIT - Could not insert new update", e);
            System.exit(-1);
        }
    }

    private static void connect() throws IOException {
        LOGGER.info("Bot is logging in...");

        DiscordApiBuilder apiBuilder = new DiscordApiBuilder()
                .setToken(SecretManager.getString(Bot.isProductionMode() ? "bot.token" : "bot.token.debugger"))
                .setRecommendedTotalShards().join();

        int totalShards = apiBuilder.getTotalShards();
        DiscordApiCollection.getInstance().init(totalShards);

        apiBuilder.loginAllShards()
                .forEach(shardFuture -> {
                            if (shardFuture.thenAccept(api -> onApiJoin(api, true))
                                    .isCompletedExceptionally())
                            {
                                LOGGER.error("EXIT - Error while connecting to the Discord servers!");
                                System.exit(-1);
                            }
                        }
                );
    }

    public static void reconnectApi(int shardId) {
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

    public static void onApiJoin(DiscordApi api, boolean startup) {
        api.setAutomaticMessageCacheCleanupEnabled(true);
        api.setMessageCacheSize(30, 30 * 60);

        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        apiCollection.insertApi(api);

        new CustomThread(() -> DBAutoChannel.getInstance().synchronize(api),
                "autochannel_synchro_shard_" + api.getCurrentShard(),
                1
        ).start();

        LOGGER.info("Shard {} connection established", api.getCurrentShard());

        if (apiCollection.allShardsConnected()) {
            if (startup) {
                updateActivity();
                DBFishery.getInstance().cleanUp();
                new WebComServer(15744);
                DBFishery.getInstance().startVCObserver();

                LOGGER.info("All shards connected successfully");
                new ScheduleEventManager().start();
                DBTracker.getInstance().start();
            } else {
                updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
            }
        }

        new DiscordEventManager().registerApi(api);
        api.addReconnectListener(event -> new CustomThread(() -> onSessionResume(event.getApi()), "reconnect").start());
    }

    public static void updateActivity() {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        int serverTotalSize = apiCollection.getServerTotalSize();
        for(DiscordApi api: apiCollection.getApis()) {
            updateActivity(api, serverTotalSize);
        }
    }

    public static void updateActivity(DiscordApi api, int serverNumber) {
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

    private static void onSessionResume(DiscordApi api) {
        LOGGER.debug("Connection has been reestablished!");
        updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
    }

}