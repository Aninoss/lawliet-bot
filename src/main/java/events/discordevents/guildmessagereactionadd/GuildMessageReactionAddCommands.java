package events.discordevents.guildmessagereactionadd;

import core.ReactionCommandCheck;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionAddAbstract;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

@DiscordEvent
public class GuildMessageReactionAddCommands extends GuildMessageReactionAddAbstract {

    @Override
    public boolean onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) throws Throwable {
        return ReactionCommandCheck.manage(event);
    }

}
