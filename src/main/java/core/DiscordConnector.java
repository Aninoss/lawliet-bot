package core;

import core.utils.StringUtil;
import events.discordevents.DiscordEventAdapter;
import events.discordevents.DiscordEventManagerDeprecated;
import events.scheduleevents.ScheduleEventManager;
import modules.BumpReminder;
import modules.FisheryVCObserver;
import modules.repair.MainRepair;
import modules.schedulers.GiveawayScheduler;
import modules.schedulers.ReminderScheduler;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.tracker.DBTracker;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SyncManager;

import javax.security.auth.login.LoginException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DiscordConnector {

    private static final DiscordConnector ourInstance = new DiscordConnector();

    public static DiscordConnector getInstance() {
        return ourInstance;
    }

    private DiscordConnector() {
        DiscordApiManager.getInstance().addShardDisconnectConsumer(this::reconnectApi);
    }

    private final DiscordEventManagerDeprecated discordEventManagerDeprecated = new DiscordEventManagerDeprecated();
    private boolean started = false;

    public void connect(int shardMin, int shardMax, int totalShards) {
        if (started) return;
        started = true;

        DiscordApiManager.getInstance().init(shardMin, shardMax, totalShards);
        DBFishery.getInstance().cleanUp();
        FisheryVCObserver.getInstance().start();

        MainLogger.get().info("Bot is logging in...");
        JDABuilder apiBuilder = createBuilder();
        RestAction.setDefaultTimeout(10, TimeUnit.SECONDS);

        for(int i = shardMin; i <= shardMax; i++) {
            try {
                apiBuilder.useSharding(i, totalShards)
                        .build();
            } catch (LoginException e) {
                MainLogger.get().error("EXIT - Login exception", e);
                System.exit(1);
            }
        }
    }

    public void reconnectApi(int shardId) {
        MainLogger.get().info("Shard {} is getting reconnected...", shardId);

        try {
            createBuilder().useSharding(shardId, DiscordApiManager.getInstance().getTotalShards())
                    .build();
        } catch (LoginException e) {
            MainLogger.get().error("EXIT - Login exception", e);
            System.exit(1);
        }
    }

    private JDABuilder createBuilder() {
        //TODO: Add SessionController for global rate limits
        return JDABuilder.createDefault(System.getenv("BOT_TOKEN"))
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .disableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new DiscordEventAdapter());
    }

    public void onApiJoin(JDA jda) {
        DiscordApiManager.getInstance().addApi(jda);
        MainLogger.get().info("Shard {} connection established", jda.getShardInfo().getShardId());

        if (DiscordApiManager.getInstance().isEverythingConnected() && !DiscordApiManager.getInstance().isFullyConnected()) {
            onConnectionCompleted();
        }

        updateActivity(jda);
        MainRepair.start(jda, 5);
    }

    private void onConnectionCompleted() {
        new ScheduleEventManager().start();
        DBTracker.getInstance().start();
        if (Bot.isProductionMode() && Bot.isPublicVersion()) BumpReminder.getInstance().start();
        ReminderScheduler.getInstance().start();
        GiveawayScheduler.getInstance().start();

        DiscordApiManager.getInstance().start();
        SyncManager.getInstance().setFullyConnected();
        MainLogger.get().info("### ALL SHARDS CONNECTED SUCCESSFULLY! ###");
    }

    public void updateActivity(JDA jda) {
        Optional<Long> serverSizeOpt = DiscordApiManager.getInstance().getGlobalServerSize();
        if (serverSizeOpt.isPresent()) {
            jda.getPresence().setActivity(Activity.watching("L.help | " + StringUtil.numToString(serverSizeOpt.get()) + " | www.lawlietbot.xyz"));
        } else {
            jda.getPresence().setActivity(Activity.watching("L.help | www.lawlietbot.xyz"));
        }
    }

    public void onSessionResume(JDA jda) {
        MainLogger.get().debug("Connection has been reestablished!");
        updateActivity(jda);
    }

    public boolean isStarted() {
        return started;
    }

}