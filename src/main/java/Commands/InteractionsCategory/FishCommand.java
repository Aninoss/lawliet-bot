package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "fish",
        emoji = "\uD83C\uDFA3",
        executable = false
)
public class FishCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/fd05547c2e5989a2fa049e1ddf47ca63/tenor.gif?itemid=13155586",
                "https://media1.tenor.com/images/133f758afa1514e781b7ddb91087a472/tenor.gif?itemid=4986744"
        };
    }

}
