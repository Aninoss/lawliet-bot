package events.discordevents.buttonclick;

import commands.CommandContainer;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotPermissionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ButtonClickAbstract;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

@DiscordEvent
public class ButtonClickCommands extends ButtonClickAbstract {

    @Override
    public boolean onButtonClick(ButtonClickEvent event) {
        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
            CommandContainer.getInstance().getListeners(OnButtonListener.class)
                    .forEach(listener -> {
                        switch (listener.check(event)) {
                            case ACCEPT -> ((OnButtonListener) listener.getCommand()).processButton(event);
                            case DENY -> {
                                EmbedBuilder eb = EmbedFactory.getEmbedError(
                                        listener.getCommand(),
                                        TextManager.getString(listener.getCommand().getLocale(), TextManager.GENERAL, "button_listener_denied", listener.getCommand().getMemberAsMention().get())
                                );
                                event.replyEmbeds(eb.build())
                                        .setEphemeral(true)
                                        .queue();
                            }
                        }
                    });
        }

        return true;
    }

}
