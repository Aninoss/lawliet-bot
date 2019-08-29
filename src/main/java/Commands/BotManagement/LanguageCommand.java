package Commands.BotManagement;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.Permission;
import General.EmbedFactory;
import MySQL.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Locale;

public class LanguageCommand extends Command implements onRecievedListener, onReactionAddListener {
    private Message message;

    private final String[] languageEmojis = new String[]{"\uD83C\uDDE9\uD83C\uDDEA", "\uD83C\uDDEC\uD83C\uDDE7"};
    private final String[] languageLocales = new String[]{"de_de", "en_us"};
    private final String[] languageArgs = new String[]{"de", "en"};

    public LanguageCommand() {
        super();
        trigger = "language";
        privateUse = false;
        botPermissions = 0;
        userPermissions = Permission.MANAGE_SERVER;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/android-settings/128/flag-icon.png";
        emoji = "\uD83C\uDF10";
        executable = true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
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
                locale = new Locale(languageLocales[language]);
            }

            DBServer.setServerLocale(event.getServer().get(), locale);
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
                locale = new Locale(languageLocales[i]);
                DBServer.setServerLocale(event.getServer().get(), locale);
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
