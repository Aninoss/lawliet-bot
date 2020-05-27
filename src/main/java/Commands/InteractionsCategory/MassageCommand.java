package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "massage",
        emoji = "\uD83D\uDC86",
        executable = true
)
public class MassageCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/71e74263a48a6e9a2c53f3bc1439c3ac/tenor.gif?itemid=12434286",
                "https://media1.tenor.com/images/8d995dfe4679d6d8f4c3c59ec39370c5/tenor.gif?itemid=12822446",
                "https://media1.tenor.com/images/4cbec203d1e480dfd79ec0b5efed742f/tenor.gif?itemid=10166717",
                "https://media1.tenor.com/images/e8825d223bc7f6aae6c31faecd1087da/tenor.gif?itemid=13749329",
                "https://media1.tenor.com/images/d0ebf560496e82f495f4d90570b5221d/tenor.gif?itemid=3542368"
        };
    }

}
