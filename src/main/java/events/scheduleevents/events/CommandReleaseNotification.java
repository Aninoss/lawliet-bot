package events.scheduleevents.events;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import constants.AssetIds;
import constants.ExceptionRunnable;
import constants.Language;
import core.Program;
import core.ShardManager;
import events.scheduleevents.ScheduleEventDaily;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

@ScheduleEventDaily
public class CommandReleaseNotification implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.publicVersion()) {
            AtomicBoolean newRelease = new AtomicBoolean(false);
            ShardManager.getLocalGuildById(AssetIds.SUPPORT_SERVER_ID)
                    .map(guild -> guild.getChannelById(StandardGuildMessageChannel.class, 557960859792441357L))
                    .ifPresent(channel -> {
                        CommandContainer.getCommandCategoryMap().values().forEach(list -> list.forEach(clazz -> {
                            Command command = CommandManager.createCommandByClass(clazz, Language.EN.getLocale(), "L.");
                            command.getReleaseDate().ifPresent(date -> {
                                if (date.isEqual(LocalDate.now())) {
                                    String message = "`L." + command.getTrigger() + "` is now publicly available!";
                                    channel.sendMessage(message).flatMap(Message::crosspost).queue();
                                    newRelease.set(true);
                                }
                            });
                        }));
                    });

            if (newRelease.get()) {
                Guild guild = ShardManager.getLocalGuildById(AssetIds.SUPPORT_SERVER_ID).get();
                TextChannel channel = guild.getTextChannelById(557960859792441357L);
                Role role = guild.getRoleById(703879430799622155L);
                channel.sendMessage(role.getAsMention())
                        .setAllowedMentions(Collections.singleton(Message.MentionType.ROLE))
                        .flatMap(Message::delete)
                        .queue();
            }
        }
    }

}
