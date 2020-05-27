package DiscordEvents.MessageCreate;

import Core.DiscordApiCollection;
import Core.TextManager;
import Core.Utils.StringUtil;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventPriority;
import DiscordEvents.EventTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.event.message.MessageCreateEvent;

@DiscordEventAnnotation(priority = EventPriority.HIGH)
public class MessageCreateSingleBotMention extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        if (StringUtil.trimString(event.getMessageContent().replace("@!", "@")).equalsIgnoreCase(DiscordApiCollection.getInstance().getYourself().getMentionTag())) {
            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
            String text = TextManager.getString(serverBean.getLocale(), TextManager.GENERAL, "bot_ping_help", serverBean.getPrefix());
            if (event.getChannel().canYouWrite()) event.getChannel().sendMessage(text).get();

            return false;
        }

        return true;
    }

}
