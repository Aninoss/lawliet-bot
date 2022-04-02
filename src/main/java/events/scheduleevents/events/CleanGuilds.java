package events.scheduleevents.events;

import java.time.LocalDate;
import java.util.List;
import constants.ExceptionRunnable;
import core.MainLogger;
import core.Program;
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
            int limit = 500;
            long guildIdOffset = 0;
            do {
                Thread.sleep(100);
                guildKickedDataList = DBGuild.getInstance().retrieveKickedData(guildIdOffset, limit);
                for (GuildKickedData guildKickedData : guildKickedDataList) {
                    try {
                        long guildId = guildKickedData.getGuildId();
                        LocalDate kicked = guildKickedData.getKicked();
                        if (ShardManager.getLocalGuildById(guildId).isPresent()) {
                            if (kicked != null) {
                                MainLogger.get().info("Guild {} has been set to \"not kicked\"", guildId);
                                DBGuild.getInstance().setKicked(guildId, null);
                            }
                        } else {
                            if (kicked == null) {
                                MainLogger.get().info("Guild {} has been set to \"kicked\"", guildId);
                                DBGuild.getInstance().setKicked(guildId, LocalDate.now());
                            } else if (LocalDate.now().isAfter(kicked.plusDays(6)) && Program.productionMode()) {
                                MainLogger.get().info("Guild data for {} has been removed", guildId);
                                DBGuild.getInstance().remove(guildId);
                            }
                        }
                    } catch (Throwable e) {
                        MainLogger.get().error("Error in guild cleaner", e);
                    }
                }
                if (guildKickedDataList.size() > 0) {
                    guildIdOffset = guildKickedDataList.get(guildKickedDataList.size() - 1).getGuildId();
                }
            } while (guildKickedDataList.size() == limit);
            MainLogger.get().info("Guild cleaner completed");
        } else {
            MainLogger.get().error("Guild cleaner failed due to missing connections");
        }
    }

}
