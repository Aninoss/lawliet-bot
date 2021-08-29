package events.scheduleevents.events;

import java.util.Calendar;
import java.util.Collections;
import constants.AssetIds;
import constants.ExternalLinks;
import core.Program;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

@ScheduleEventDaily
public class FeatureRequestsRefillNotification implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && Program.publicVersion()) {
            String message = "It's the beginning of a new week, therefore you can now boost again for your favorite Lawliet feature requests: " + ExternalLinks.FEATURE_REQUESTS_WEBSITE;
            ShardManager.getLocalGuildById(AssetIds.SUPPORT_SERVER_ID)
                    .map(server -> server.getTextChannelById(557960859792441357L))
                    .ifPresent(channel -> {
                        channel.sendMessage(message).flatMap(Message::crosspost).queue();

                        Role role = channel.getGuild().getRoleById(703879430799622155L);
                        channel.sendMessage(role.getAsMention())
                                .allowedMentions(Collections.singleton(Message.MentionType.ROLE))
                                .flatMap(Message::delete).queue();
                    });
        }
    }

}
