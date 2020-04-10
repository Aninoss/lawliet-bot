package Commands.ManagementCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Locales;
import Constants.Permission;
import Core.EmbedFactory;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.util.Locale;

@CommandProperties(
        trigger = "language",
        userPermissions = Permission.MANAGE_SERVER,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/android-settings/128/flag-icon.png",
        emoji = "\uD83C\uDF10",
        executable = true,
        aliases = {"sprache", "lang"}
)
public class LanguageCommand extends Command implements OnReactionAddListener {

    private Message message;

    private final String[] languageEmojis = new String[]{"\uD83C\uDDE9\uD83C\uDDEA", "\uD83C\uDDEC\uD83C\uDDE7"};
    private final String[] languageLocales = Locales.LIST;
    private final String[] languageArgs = new String[]{"de", "en"};

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.length() > 0) {
            int language = -1;
            for(int i=0; i<languageArgs.length; i++) {
                String str = languageArgs[i];
                if (followedString.equalsIgnoreCase(str)) language = i;
            }

            if (language == -1) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("invalid", followedString)));
                return false;
            } else {
                setLocale(new Locale(languageLocales[language]));
            }

            DBServer.getInstance().getBean(event.getServer().get().getId()).setLocale(getLocale());
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("set"))).get();
            return true;
        } else {
            message = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("reaction"))).get();
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
            if (event.getEmoji().getMentionTag().equalsIgnoreCase(str)) {
                setLocale(new Locale(languageLocales[i]));
                DBServer.getInstance().getBean(event.getServer().get().getId()).setLocale(getLocale());
                getReactionMessage().edit(EmbedFactory.getCommandEmbedSuccess(this, getString("set"))).get();
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
