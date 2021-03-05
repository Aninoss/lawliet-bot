package events.discordevents.guildmessagereactionremove;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionRemoveAbstract;
import modules.ReactionCommandCheck;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;

@DiscordEvent()
public class GuildMessageReactionRemoveCommands extends GuildMessageReactionRemoveAbstract {

    @Override
    public boolean onGuildMessageReactionRemove(ReactionRemoveEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
