package commands.runnables.utilitycategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;

import commands.Command;
import constants.PermissionDeprecated;
import core.EmbedFactory;
import core.utils.DiscordUtil;
import core.utils.StringUtil;
import mysql.modules.autoquote.DBAutoQuote;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "autoquote",
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = true
)
public class AutoQuoteCommand extends Command implements OnReactionAddListener {

    private Message message;

    private final String[] activeArgs = new String[]{"off", "on"};

    public AutoQuoteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.length() > 0) {
            int option = -1;
            for(int i=0; i < activeArgs.length; i++) {
                String str = activeArgs[i];
                if (followedString.equalsIgnoreCase(str)) option = i;
            }

            if (option == -1) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("invalid", followedString)));
                return false;
            }

            boolean active = option == 1;
            DBAutoQuote.getInstance().retrieve(event.getServer().get().getId()).setActive(active);
            event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set", active))).get();
            return true;
        } else {
            String onOffText = StringUtil.getOnOffForBoolean(getLocale(), DBAutoQuote.getInstance().retrieve(event.getServer().get().getId()).isActive());
            message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("reaction", onOffText))).get();
            for(int i = 0; i < 2; i++) {
                message.addReaction(StringUtil.getEmojiForBoolean(i == 1));
            }
            return true;
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        for(int i = 0; i < 2; i++) {
            String str = StringUtil.getEmojiForBoolean(i == 1);
            if (DiscordUtil.emojiIsString(event.getEmoji(), str)) {
                boolean active = i == 1;
                DBAutoQuote.getInstance().retrieve(event.getServer().get().getId()).setActive(active);
                getReactionMessage().edit(EmbedFactory.getEmbedDefault(this, getString("set", active))).get();
                removeReactionListener(getReactionMessage());
                return;
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}
}
