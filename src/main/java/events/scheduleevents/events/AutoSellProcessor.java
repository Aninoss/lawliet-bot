package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import constants.ExceptionRunnable;
import core.MainLogger;
import core.ShardManager;
import events.scheduleevents.ScheduleEventFixedRate;
import modules.fishery.FisheryStatus;
import mysql.modules.autosell.AutoSellData;
import mysql.modules.autosell.DBAutoSell;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.entities.Guild;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class AutoSellProcessor implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        AtomicInteger actions = new AtomicInteger(0);

        ShardManager.getLocalGuilds().stream()
                .filter(guild -> {
                    try {
                        return DBGuild.getInstance().retrieve(guild.getIdLong()).getFisheryStatus() == FisheryStatus.ACTIVE;
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not get server data", e);
                    }
                    return false;
                })
                .forEach(guild -> {
                    try {
                        process(guild, actions);
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not process auto sell", e);
                    }
                });

        MainLogger.get().info("Auto Sell - {} Actions", actions.get());
    }

   private void process(Guild guild, AtomicInteger actions) {
        FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(guild.getIdLong());
        AutoSellData autoSellData = DBAutoSell.getInstance().retrieve();

        guild.getMembers().stream()
                .filter(member -> autoSellData.getThreshold(member.getIdLong()) != null)
                .forEach(member -> {
                    if (fisheryGuildData.getMemberData(member.getIdLong()).processAutoSell()) {
                        actions.incrementAndGet();
                    }
                });
    }

}
