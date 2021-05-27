package events.discordevents.guildcomponentinteraction;

import commands.CommandContainer;
import commands.listeners.OnButtonListener;
import core.buttons.GuildComponentInteractionEvent;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildComponentInteractionAbstract;

@DiscordEvent
public class GuildComponentInteractionCommands extends GuildComponentInteractionAbstract {

    @Override
    public boolean onGuildComponentInteraction(GuildComponentInteractionEvent event) throws Throwable {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            CommandContainer.getInstance().getListeners(OnButtonListener.class).stream()
                    .filter(listener -> listener.check(event))
                    .forEach(listener -> ((OnButtonListener) listener.getCommand()).processButton(event));
        }

        return true;
    }

}
