package Commands.General;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.TextManager;
import General.Tools;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Random;

public class RollCommand extends Command implements onRecievedListener {
    public RollCommand() {
        super();
        trigger = "roll";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat-one-color/64/die-icon.png";
        emoji = "\uD83C\uDFB2";
        executable = true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Random n = new Random();
        double drawn, border;
        boolean userMentioned = true;

        if (followedString.length() == 0 || !Tools.stringIsNumeric(followedString)){
            border = 6;
            userMentioned = false;
        }
        else {
            border = Double.parseDouble(followedString);
            if (border < 2) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.GENERAL,"too_small", "2"))).get();
                return false;
            }
            if (border > 999999999999999999.0) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.GENERAL,"too_large", "999999999999999999"))).get();
                return false;
            }
        }

        drawn = Math.floor(n.nextDouble()*border)+1;

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                getString("result",event.getMessage().getAuthor().getDisplayName(), String.valueOf((long) drawn),String.valueOf((long) border)));
        if (!userMentioned) eb.setFooter(getString("noarg"));
        event.getChannel().sendMessage(eb).get();
        return true;
    }
}
