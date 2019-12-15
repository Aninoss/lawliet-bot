package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "fish",
        emoji = "\uD83C\uDFA3",
        executable = false
)
public class FishCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public FishCommand() {
        super("https://media1.tenor.com/images/fd05547c2e5989a2fa049e1ddf47ca63/tenor.gif?itemid=13155586",
                "https://media1.tenor.com/images/133f758afa1514e781b7ddb91087a472/tenor.gif?itemid=4986744"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
