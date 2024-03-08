package events.scheduleevents.events;

import constants.AssetIds;
import constants.ExceptionRunnable;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import events.scheduleevents.ScheduleEventFixedRate;
import net.dv8tion.jda.api.entities.Role;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.HOURS)
public class AnicordKickOldUnverifiedMembers implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.productionMode() && Program.publicInstance()) {
            ShardManager.getLocalGuildById(AssetIds.ANICORD_SERVER_ID).ifPresent(guild -> {
                Role memberRole = guild.getRoleById(462410205288726531L);
                AtomicInteger counter = new AtomicInteger(0);
                guild.getMembers().forEach(member -> {
                    if (!member.getRoles().contains(memberRole) &&
                            !member.getUser().isBot() &&
                            member.hasTimeJoined() &&
                            member.getTimeJoined().toInstant().isBefore(Instant.now().minus(Duration.ofDays(3)))
                    ) {
                        MainLogger.get().info("Kicked Unverified Member: " + member.getUser().getAsTag());
                        counter.incrementAndGet();
                        guild.kick(member).reason("Unverified").queue();
                    }
                });
                MainLogger.get().info("Removed Members: " + counter.get());
            });
        }
    }

}
