package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "merkel",
        emoji = "\uD83C\uDDE9\uD83C\uDDEA",
        executable = false
)
public class MerkelCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public MerkelCommand() {
        super("https://cdn.discordapp.com/attachments/499629904380297226/590539990455418885/merkel.png");
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
