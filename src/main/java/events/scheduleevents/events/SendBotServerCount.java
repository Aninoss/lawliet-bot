package events.scheduleevents.events;

import core.Bot;
import core.DiscordApiManager;
import events.scheduleevents.ScheduleEventFixedRate;
import core.schedule.ScheduleInterface;
import websockets.*;

import java.time.temporal.ChronoUnit;

@ScheduleEventFixedRate(rateValue = 5, rateUnit = ChronoUnit.MINUTES)
public class SendBotServerCount implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode() && Bot.isPublicVersion() && Bot.getClusterId() == 0 && DiscordApiManager.getInstance().isEverythingConnected()) {
            int totalServers = DiscordApiManager.getInstance().getGlobalServerSize();

            TopGG.getInstance().updateServerCount(totalServers);
            Botsfordiscord.updateServerCount(totalServers);
            BotsOnDiscord.updateServerCount(totalServers);
            Discordbotlist.updateServerCount(totalServers);
            Discordbotsgg.updateServerCount(totalServers);
        }
    }

}