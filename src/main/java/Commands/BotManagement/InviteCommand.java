package Commands.BotManagement;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Settings;
import General.EmbedFactory;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class InviteCommand extends Command implements onRecievedListener {
    public InviteCommand() {
        super();
        trigger = "invite";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/thehoth/seo/128/seo-chain-link-icon.png";
        emoji = "\uD83D\uDD17";
        executable = true;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Thread.sleep(1000 * 10);

        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("template", Settings.BOT_INVITE_URL))).get();
        return true;
    }
}
