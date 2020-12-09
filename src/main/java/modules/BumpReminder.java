package modules;

import constants.AssetIds;
import core.DiscordApiCollection;
import core.schedule.MainScheduler;
import core.utils.TimeUtil;
import mysql.modules.bump.DBBump;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class BumpReminder {

    private static final BumpReminder ourInstance = new BumpReminder();

    public static BumpReminder getInstance() {
        return ourInstance;
    }

    private BumpReminder() {
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(BumpReminder.class);

    private boolean started = false;
    private boolean countdownRunning = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            Instant nextBump = DBBump.getNextBump();
            long milis = TimeUtil.getMilisBetweenInstants(Instant.now(), nextBump);
            startCountdown(milis);
        } catch (Throwable e) {
            LOGGER.error("Exception on bump reminder init");
        }
    }

    public void startCountdown(long milis) {
        if (countdownRunning) return;
        countdownRunning = true;

        final long ANINOSS_SERVER_ID = AssetIds.ANICORD_SERVER_ID;
        final long BUMP_CHANNEL_ID = 713849992611102781L;

        MainScheduler.getInstance().schedule(milis, "anicord_bump", () -> {
            DiscordApiCollection.getInstance().getServerById(ANINOSS_SERVER_ID)
                    .flatMap(server -> server.getTextChannelById(BUMP_CHANNEL_ID))
                    .ifPresent(channel -> {
                        channel.sendMessage("<@&755828541886693398> Der Server ist wieder bereit fürs Bumpen! Schreibt `!d bump`").exceptionally(ExceptionLogger.get());
                        countdownRunning = false;
                    });
        });
    }

}
