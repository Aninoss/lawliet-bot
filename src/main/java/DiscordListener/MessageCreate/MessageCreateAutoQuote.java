package DiscordListener.MessageCreate;

import Commands.GimmicksCategory.QuoteCommand;
import Core.ExceptionHandler;
import Core.Mention.MentionUtil;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.MessageCreateAbstract;
import MySQL.Modules.AutoQuote.DBAutoQuote;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;

@DiscordListenerAnnotation()
public class MessageCreateAutoQuote extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        if (event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
            Locale locale = serverBean.getLocale();
            ArrayList<Message> messages = MentionUtil.getMessagesURL(event.getMessage(), event.getMessage().getContent()).getList();
            if (messages.size() > 0 && DBAutoQuote.getInstance().getBean(event.getServer().get().getId()).isActive()) {
                try {
                    for (int i = 0; i < Math.min(3, messages.size()); i++) {
                        Message message = messages.get(i);
                        QuoteCommand quoteCommand = new QuoteCommand();
                        quoteCommand.setLocale(locale);
                        quoteCommand.setPrefix(serverBean.getPrefix());
                        quoteCommand.postEmbed(event.getServerTextChannel().get(), message, true);
                    }
                } catch (Throwable throwable) {
                    ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
                }
            }
        }

        return true;
    }

}
