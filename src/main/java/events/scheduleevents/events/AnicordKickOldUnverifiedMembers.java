package events.scheduleevents.events;

import constants.AssetIds;
import core.Bot;
import core.DiscordApiManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventHourly;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@ScheduleEventHourly
public class AnicordKickOldUnverifiedMembers implements ScheduleInterface {

    private final static Logger LOGGER = LoggerFactory.getLogger(AnicordKickOldUnverifiedMembers.class);

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode() && Bot.isPublicVersion()) {
            DiscordApiManager.getInstance().getLocalServerById(AssetIds.ANICORD_SERVER_ID).ifPresent(server -> {
                Role memberRole = server.getRoleById(462410205288726531L).get();
                AtomicInteger counter = new AtomicInteger(0);
                server.getMembers().forEach(member -> {
                    if (!memberRole.hasUser(member) &&
                            !member.isBot() &&
                            member.getJoinedAtTimestamp(server).map(instant -> instant.isBefore(Instant.now().minus(Duration.ofDays(3)))).orElse(true)
                    ) {
                        LOGGER.info("Kicked Unverified Member: " + member.getDiscriminatedName());
                        counter.incrementAndGet();
                        server.kickUser(member).exceptionally(ExceptionLogger.get());
                    }
                });
                LOGGER.info("Removed Members: " + counter.get());
            });
        }
    }

}
