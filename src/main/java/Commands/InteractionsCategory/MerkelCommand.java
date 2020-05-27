package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "merkel",
        emoji = "\uD83C\uDDE9\uD83C\uDDEA",
        executable = true
)
public class MerkelCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://cdn.discordapp.com/attachments/499629904380297226/590539990455418885/merkel.png"};
    }

}
