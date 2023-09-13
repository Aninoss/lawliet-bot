package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildKickedData;
import net.dv8tion.jda.api.entities.Guild;

import java.time.LocalDate;
import java.util.List;

@ScheduleEventDaily
public class CleanGuilds implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        //execute(); TODO: Deactivate temporarily until de-coupling of general guild database table is complete
    }

    public static void execute() throws InterruptedException {
        if (!Program.publicVersion()) {
            for (Guild guild : ShardManager.getLocalGuilds()) {
                MainLogger.get().info("Guild {} has been set to \"not kicked\"", guild.getIdLong());
                DBGuild.getInstance().setKicked(guild.getIdLong(), null);
            }
            MainLogger.get().info("Guild cleaner completed");
            return;
        }

        if (!ShardManager.isEverythingConnected()) {
            MainLogger.get().error("Guild cleaner failed due to missing connections");
            return;
        }

        List<GuildKickedData> guildKickedDataList;
        int limit = 1000;
        long guildIdOffset = 0;
        do {
            Thread.sleep(50);
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
    }

}
