package events.discordevents.guildmessagereceived;

import core.ShardManager;
import core.TextManager;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEvent(priority = EventPriority.MEDIUM)
public class GuildMessageReceivedSingleBotMention extends GuildMessageReceivedAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        if (StringUtil.trimString(event.getMessageContent().replace("@!", "@")).equalsIgnoreCase(ShardManager.getInstance().getSelf().getMentionTag())) {
            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());

            String text = TextManager.getString(serverBean.getLocale(), TextManager.GENERAL, "bot_ping_help", serverBean.getPrefix());
            if (event.getChannel().canYouWrite())
                event.getChannel().sendMessage(text).get();

            return false;
        }

        return true;
    }

}
