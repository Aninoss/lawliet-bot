package events.discordevents.buttonclick;

import commands.CommandContainer;
import commands.listeners.OnButtonListener;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

@DiscordEvent
public class ButtonClickCommands extends ButtonClickAbstract {

    @Override
    public boolean onButtonClick(ButtonClickEvent event) throws Throwable {
        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
            CommandContainer.getInstance().getListeners(OnButtonListener.class).stream()
                    .filter(listener -> listener.check(event))
                    .forEach(listener -> ((OnButtonListener) listener.getCommand()).processButton(event));
        }

        return true;
    }

}
