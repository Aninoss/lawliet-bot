package events.scheduleevents.events;

import constants.AssetIds;
import core.Bot;
import core.ShardManager;
import core.MainLogger;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventHourly;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.util.logging.ExceptionLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@ScheduleEventHourly
public class AnicordKickOldUnverifiedMembers implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode() && Bot.isPublicVersion()) {
            ShardManager.getInstance().getLocalGuildById(AssetIds.ANICORD_SERVER_ID).ifPresent(server -> {
                Role memberRole = server.getRoleById(462410205288726531L).get();
                AtomicInteger counter = new AtomicInteger(0);
                server.getMembers().forEach(member -> {
                    if (!memberRole.hasUser(member) &&
                            !member.isBot() &&
                            member.getJoinedAtTimestamp(server).map(instant -> instant.isBefore(Instant.now().minus(Duration.ofDays(3)))).orElse(true)
                    ) {
                        MainLogger.get().info("Kicked Unverified Member: " + member.getDiscriminatedName());
                        counter.incrementAndGet();
                        server.kickUser(member).exceptionally(ExceptionLogger.get());
                    }
                });
                MainLogger.get().info("Removed Members: " + counter.get());
            });
        }
    }

}
