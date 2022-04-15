package modules;

import java.time.Instant;
import java.util.Collections;
import constants.AssetIds;
import core.MainLogger;
import core.ShardManager;
import core.schedule.MainScheduler;
import core.utils.TimeUtil;
import mysql.modules.bump.DBBump;
import net.dv8tion.jda.api.entities.Message;

public class BumpReminder {

    private static boolean countdownRunning = false;

    public static void start() {
        try {
            Instant nextBump = DBBump.getNextBump();
            long millis = TimeUtil.getMillisBetweenInstants(Instant.now(), nextBump);
            startCountdown(millis);
        } catch (Throwable e) {
            MainLogger.get().error("Exception on bump reminder init");
        }
    }

    public static void startCountdown(long millis) {
        if (countdownRunning) return;
        countdownRunning = true;

        final long ANINOSS_SERVER_ID = AssetIds.ANICORD_SERVER_ID;
        final long BUMP_CHANNEL_ID = 713849992611102781L;

        MainScheduler.schedule(millis, "anicord_bump", () -> {
            ShardManager.getLocalGuildById(ANINOSS_SERVER_ID)
                    .map(guild -> guild.getTextChannelById(BUMP_CHANNEL_ID))
                    .ifPresent(channel -> {
                        channel.sendMessage("<@&755828541886693398> Der Server ist wieder bereit fürs Bumpen! Führt `/bump` aus!")
                                .allowedMentions(Collections.singleton(Message.MentionType.ROLE))
                                .queue();
                        countdownRunning = false;
                    });
        });
    }

}
