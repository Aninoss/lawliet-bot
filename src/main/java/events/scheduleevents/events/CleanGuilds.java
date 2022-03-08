package events.scheduleevents.events;

import java.time.LocalDate;
import java.util.List;
import constants.ExceptionRunnable;
import core.MainLogger;
import core.ShardManager;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildKickedData;

@ScheduleEventDaily
public class CleanGuilds implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() throws InterruptedException {
        if (ShardManager.isEverythingConnected()) {
            List<GuildKickedData> guildKickedDataList;
            int limit = 100;
            int offset = 0;
            do {
                Thread.sleep(100);
                guildKickedDataList = DBGuild.getInstance().retrieveKickedData(offset, limit);
                for (GuildKickedData guildKickedData : guildKickedDataList) {
                    long guildId = guildKickedData.getGuildId();
                    LocalDate kicked = guildKickedData.getKicked();
                    if (ShardManager.getLocalGuildById(guildId).isPresent()) {
                        if (kicked != null) {
                            MainLogger.get().info("Guild {} has been set to \"not kicked\"", guildId);
                            DBGuild.getInstance().retrieve(guildId).setKicked(null);
                        }
                    } else {
                        if (kicked == null) {
                            MainLogger.get().info("Guild {} has been set to \"kicked\"", guildId);
                            DBGuild.getInstance().retrieve(guildId).setKicked(LocalDate.now());
                        } else if (LocalDate.now().isAfter(kicked.plusDays(6))) {
                            MainLogger.get().info("Guild data for {} has been removed", guildId);
                            //DBGuild.getInstance().remove(guildId); //TODO: uncomment
                        }
                    }
                }
                offset += limit;
            } while (guildKickedDataList.size() == limit);
            MainLogger.get().info("Guild cleaner completed");
        } else {
            MainLogger.get().error("Guild cleaner failed due to missing connections");
        }
    }

}
