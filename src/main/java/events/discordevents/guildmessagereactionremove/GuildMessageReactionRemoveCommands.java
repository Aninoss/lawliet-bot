package events.discordevents.guildmessagereactionremove;

import core.ReactionCommandCheck;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionRemoveAbstract;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

@DiscordEvent
public class GuildMessageReactionRemoveCommands extends GuildMessageReactionRemoveAbstract {

    @Override
    public boolean onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        return ReactionCommandCheck.manage(event);
    }

}
