package events.discordevents.guildmessagereactionadd;

import core.ReactionCommandCheck;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageReactionAddAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

@DiscordEvent
public class GuildMessageReactionAddCommands extends GuildMessageReactionAddAbstract {

    @Override
    public boolean onGuildMessageReactionAdd(MessageReactionAddEvent event, EntityManagerWrapper entityManager) throws Throwable {
        return ReactionCommandCheck.manage(event, entityManager);
    }

}
