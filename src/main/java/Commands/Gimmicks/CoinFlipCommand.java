package Commands.Gimmicks;

import CommandListeners.*;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.TextManager;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Random;

@CommandProperties(
        trigger = "coinflip",
        thumbnail = "https://s3-us-west-2.amazonaws.com/slack-files2/avatars/2016-02-29/23582839171_4e2343645d65907a8f97_512.png",
        emoji = "\uD83D\uDCB0",
        executable = true,
        aliases = {"coin"}
)
public class CoinFlipCommand extends Command implements onRecievedListener {

    public CoinFlipCommand() { super(); }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Random rand = new Random();
        int n = rand.nextInt(2);

        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("result",event.getMessage().getAuthor().getDisplayName(),
                TextManager.getString(getLocale(),TextManager.COMMANDS,"coinflip_result"+n)))).get();

        return true;
    }

}
