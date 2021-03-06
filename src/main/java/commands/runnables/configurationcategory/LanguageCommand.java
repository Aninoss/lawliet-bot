package commands.runnables.configurationcategory;

import commands.listeners.*;
import commands.Command;
import constants.Locales;
import constants.PermissionDeprecated;
import core.EmbedFactory;
import core.utils.DiscordUtil;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.util.Locale;

@CommandProperties(
        trigger = "language",
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "\uD83C\uDF10",
        executableWithoutArgs = true,
        aliases = { "sprache", "lang" }
)
public class LanguageCommand extends Command implements OnReactionAddListener {

    private Message message;

    private final String[] languageEmojis = new String[]{"\uD83C\uDDE9\uD83C\uDDEA", "\uD83C\uDDF7\uD83C\uDDFA", "\uD83C\uDDEC\uD83C\uDDE7"};
    private final String[] languageLocales = Locales.LIST;
    private final String[] languageArgs = new String[]{"de", "ru", "en"};

    public LanguageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.length() > 0) {
            int language = -1;
            for(int i=0; i<languageArgs.length; i++) {
                String str = languageArgs[i];
                if (followedString.equalsIgnoreCase(str)) language = i;
            }

            if (language == -1) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("invalid", followedString)));
                return false;
            } else {
                setLocale(new Locale(languageLocales[language]));
            }

            DBServer.getInstance().retrieve(event.getServer().get().getId()).setLocale(getLocale());
            event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set"))).get();
            return true;
        } else {
            message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("reaction"))).get();
            for(String str: languageEmojis) {
                message.addReaction(str);
            }
            return true;
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        for(int i=0; i<languageEmojis.length; i++) {
            String str = languageEmojis[i];
            if (DiscordUtil.emojiIsString(event.getEmoji(), str)) {
                setLocale(new Locale(languageLocales[i]));
                DBServer.getInstance().retrieve(event.getServer().get().getId()).setLocale(getLocale());
                getReactionMessage().edit(EmbedFactory.getEmbedDefault(this, getString("set"))).get();
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
