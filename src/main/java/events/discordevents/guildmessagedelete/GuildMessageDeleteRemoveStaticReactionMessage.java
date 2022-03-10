package events.discordevents.guildmessagedelete;

import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageDeleteAbstract;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;

@DiscordEvent(allowBannedUser = true, allowBots = true)
public class GuildMessageDeleteRemoveStaticReactionMessage extends GuildMessageDeleteAbstract {

    @Override
    public boolean onGuildMessageDelete(MessageDeleteEvent event) {
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).remove(event.getMessageIdLong());
        return true;
    }

}
