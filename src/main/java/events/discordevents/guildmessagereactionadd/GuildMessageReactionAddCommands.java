package events.discordevents.guildmessagereactionadd;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionAddAbstract;
import modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionAddEvent;

@DiscordEvent()
public class GuildMessageReactionAddCommands extends GuildMessageReactionAddAbstract {

    @Override
    public boolean onReactionAdd(ReactionAddEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
