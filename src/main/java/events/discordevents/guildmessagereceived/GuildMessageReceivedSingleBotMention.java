package events.discordevents.guildmessagereceived;

import core.ShardManager;
import core.TextManager;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.MEDIUM)
public class GuildMessageReceivedSingleBotMention extends GuildMessageReceivedAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        if (event.getMessageContent().replace("@!", "@").trim().equalsIgnoreCase(ShardManager.getInstance().getSelf().getMentionTag())) {
            GuildBean guildBean = DBServer.getInstance().retrieve(event.getServer().get().getId());

            String text = TextManager.getString(guildBean.getLocale(), TextManager.GENERAL, "bot_ping_help", guildBean.getPrefix());
            if (event.getChannel().canYouWrite())
                event.getChannel().sendMessage(text).get();

            return false;
        }

        return true;
    }

}
