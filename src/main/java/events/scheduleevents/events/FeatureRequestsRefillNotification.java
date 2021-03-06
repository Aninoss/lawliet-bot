package events.scheduleevents.events;

import constants.AssetIds;
import constants.ExternalLinks;
import core.Bot;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import java.util.Calendar;
import java.util.Optional;

@ScheduleEventDaily
public class FeatureRequestsRefillNotification implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && Bot.isPublicVersion()) {
            String message = "It's the beginning of a new week, therefore you can now boost again for your favorite Lawliet feature requests: " + ExternalLinks.FEATURE_REQUESTS_WEBSITE;
            ShardManager.getInstance().getLocalGuildById(AssetIds.SUPPORT_SERVER_ID)
                    .map(server -> server.getTextChannelById(557960859792441357L))
                    .ifPresent(channel -> {
                        channel.sendMessage(message).queue();

                        Role role = channel.getGuild().getRoleById(703879430799622155L);
                        channel.sendMessage(role.getAsMention())
                                .flatMap(Message::delete).queue();
                    });
        }
    }

}
