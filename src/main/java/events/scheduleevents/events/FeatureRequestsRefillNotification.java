package events.scheduleevents.events;

import constants.AssetIds;
import constants.ExternalLinks;
import core.DiscordApiCollection;
import events.scheduleevents.ScheduleEventDaily;
import core.schedule.ScheduleInterface;
import org.javacord.api.util.logging.ExceptionLogger;
import java.util.Calendar;

@ScheduleEventDaily
public class FeatureRequestsRefillNotification implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            String message = "It's the beginning of a new week, therefore everyone can now boost again for their favorite Lawliet feature requests: " + ExternalLinks.FEATURE_REQUESTS_WEBSITE;
            DiscordApiCollection.getInstance().getServerById(AssetIds.SUPPORT_SERVER_ID)
                    .flatMap(server -> server.getTextChannelById(557960859792441357L))
                    .ifPresent(channel -> channel.sendMessage(message).exceptionally(ExceptionLogger.get()));
        }
    }

}
