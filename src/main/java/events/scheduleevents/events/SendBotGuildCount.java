package events.scheduleevents.events;

import java.time.temporal.ChronoUnit;
import core.Bot;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventFixedRate;
import websockets.*;

@ScheduleEventFixedRate(rateValue = 5, rateUnit = ChronoUnit.MINUTES)
public class SendBotGuildCount implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode() && Bot.isPublicVersion() && Bot.getClusterId() == 1) {
            ShardManager.getInstance().getGlobalGuildSize().ifPresent(totalServers -> {
                TopGG.getInstance().updateServerCount(totalServers);
                Botsfordiscord.updateServerCount(totalServers);
                BotsOnDiscord.updateServerCount(totalServers);
                Discordbotlist.updateServerCount(totalServers);
                Discordbotsgg.updateServerCount(totalServers);
            });
        }
    }

}