package events.scheduleevents.events;

import constants.AssetIds;
import constants.ExternalLinks;
import core.Bot;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.Calendar;

@ScheduleEventDaily
public class FeatureRequestsRefillNotification implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && Bot.isPublicVersion()) {
            String message = "It's the beginning of a new week, therefore you can now boost again for your favorite Lawliet feature requests: " + ExternalLinks.FEATURE_REQUESTS_WEBSITE;
            ShardManager.getInstance().getLocalGuildById(AssetIds.SUPPORT_SERVER_ID)
                    .flatMap(server -> server.getTextChannelById(557960859792441357L))
                    .ifPresent(channel -> {
                        channel.sendMessage(message)
                                .exceptionally(ExceptionLogger.get());

                        Role role = channel.getServer().getRoleById(703879430799622155L).get();
                        channel.sendMessage(role.getMentionTag())
                                .thenAccept(m -> m.delete().exceptionally(ExceptionLogger.get()));
                    });
        }
    }

}
