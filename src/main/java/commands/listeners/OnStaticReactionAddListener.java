package commands.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

public interface OnStaticReactionAddListener {

    void onStaticReactionAdd(Message message, GuildMessageReactionAddEvent event) throws Throwable;

    String titleStartIndicator();

}