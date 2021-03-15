package events.scheduleevents.events;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Locale;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import constants.AssetIds;
import constants.Locales;
import core.Program;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

@ScheduleEventDaily
public class CommandReleaseNotification implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Program.isPublicVersion()) {
            CommandContainer.getInstance().getCommandCategoryMap().values().forEach(list -> list.forEach(clazz -> {
                Command command = CommandManager.createCommandByClass(clazz, new Locale(Locales.EN), "L.");
                command.getReleaseDate().ifPresent(date -> {
                    if (date.isEqual(LocalDate.now())) {
                        String message = "`L." + command.getTrigger() + "` is now publicly available!";
                        ShardManager.getInstance().getLocalGuildById(AssetIds.SUPPORT_SERVER_ID)
                                .map(guild -> guild.getTextChannelById(557960859792441357L))
                                .ifPresent(channel -> {
                                    channel.sendMessage(message).queue(); //TODO: crosspost

                                    Role role = channel.getGuild().getRoleById(703879430799622155L);
                                    channel.sendMessage(role.getAsMention())
                                            .allowedMentions(Collections.singleton(Message.MentionType.ROLE))
                                            .flatMap(Message::delete).queue();
                                });
                    }
                });
            }));
        }
    }

}
