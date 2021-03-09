package events.scheduleevents.events;

import java.time.LocalDate;
import java.util.Locale;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import constants.AssetIds;
import constants.Locales;
import core.Bot;
import core.ShardManager;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;

@ScheduleEventDaily
public class CommandReleaseNotification implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isPublicVersion()) {
            CommandContainer.getInstance().getCommandCategoryMap().values().forEach(list -> list.forEach(clazz -> {
                Command command = CommandManager.createCommandByClass(clazz, new Locale(Locales.EN), "L.");
                command.getReleaseDate().ifPresent(date -> {
                    if (date.isEqual(LocalDate.now())) {
                        String message = "<@&703879430799622155> `L." + command.getTrigger() + "` is now publicly available!";
                        ShardManager.getInstance().getLocalGuildById(AssetIds.SUPPORT_SERVER_ID)
                                .map(guild -> guild.getTextChannelById(557960859792441357L))
                                .ifPresent(channel -> channel.sendMessage(message).queue());
                    }
                });
            }));
        }
    }

}
