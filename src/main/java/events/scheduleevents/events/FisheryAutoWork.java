package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import constants.FisheryGear;
import constants.FisheryStatus;
import core.MainLogger;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.autowork.DBAutoWork;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class FisheryAutoWork implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        AtomicInteger actions = new AtomicInteger(0);
        ShardManager.getInstance().getLocalGuilds().stream()
                .filter(guild -> {
                    try {
                        return DBGuild.getInstance().retrieve(guild.getIdLong()).getFisheryStatus() == FisheryStatus.ACTIVE;
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not get server bean", e);
                    }
                    return false;
                })
                .forEach(guild -> {
                    try {
                        manageAutoWork(guild, actions);
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not manage auto work", e);
                    }
                });
        MainLogger.get().info("Auto Work - {} Actions", actions.get());
    }

    private void manageAutoWork(Guild guild, AtomicInteger actions) {
        FisheryGuildData serverBean = DBFishery.getInstance().retrieve(guild.getIdLong());
        guild.getMembers().stream()
                .filter(member -> DBAutoWork.getInstance().retrieve().isActive(member.getIdLong()))
                .map(member -> serverBean.getMemberData(member.getIdLong()))
                .filter(fisheryMemberBean -> fisheryMemberBean.checkNextWork().isEmpty())
                .forEach(fisheryMemberBean -> {
                    long coins = fisheryMemberBean.getMemberGear(FisheryGear.WORK).getEffect();
                    fisheryMemberBean.changeValues(0, coins);
                    fisheryMemberBean.setWorkDone();
                    actions.incrementAndGet();
                });
    }

}
