package commands.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

public interface OnStaticReactionRemoveListener {

    void onStaticReactionRemove(Message message, GuildMessageReactionRemoveEvent event) throws Throwable;

    String titleStartIndicator();

}