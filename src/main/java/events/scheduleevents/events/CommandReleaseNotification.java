package events.scheduleevents.events;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import constants.AssetIds;
import constants.Locales;
import core.Bot;
import core.DiscordApiCollection;
import core.schedule.ScheduleInterface;
import events.scheduleevents.ScheduleEventDaily;
import org.javacord.api.util.logging.ExceptionLogger;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Locale;

@ScheduleEventDaily
public class CommandReleaseNotification implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isPublicVersion()) {
            CommandContainer.getInstance().getCommandCategoryMap().values().forEach(list -> list.forEach(clazz -> {
                try {
                    Command command = CommandManager.createCommandByClass(clazz, new Locale(Locales.EN), "L.");
                    command.getReleaseDate().ifPresent(date -> {
                        if (date.isEqual(LocalDate.now())) {
                            String message = "<@&703879430799622155> `L." + command.getTrigger() + "` is now publicly available!";
                            DiscordApiCollection.getInstance().getServerById(AssetIds.SUPPORT_SERVER_ID)
                                    .flatMap(server -> server.getTextChannelById(557960859792441357L))
                                    .ifPresent(channel -> channel.sendMessage(message).exceptionally(ExceptionLogger.get()));
                        }
                    });
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }));
        }
    }

}
