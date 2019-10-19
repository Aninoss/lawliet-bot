package Commands.General;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.TextManager;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;

import javax.xml.soap.Text;
import java.time.Duration;
import java.time.Instant;

@CommandProperties(
        trigger = "say",
        emoji = "\uD83D\uDCAC",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/arrow-refresh-4-icon.png",
        executable = false,
        aliases = {"repeat"}
)
public class SayCommand extends Command implements onRecievedListener {

    public SayCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL,"no_args"))).get();
            return false;
        }

        event.getChannel().sendMessage(EmbedFactory.getEmbed().setDescription(followedString)).get();
        return true;
    }

}
