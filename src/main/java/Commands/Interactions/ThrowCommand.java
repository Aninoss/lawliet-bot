package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "throw",
        emoji = "\uD83D\uDCA8",
        executable = false
)
public class ThrowCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public ThrowCommand() {
        super("https://media1.tenor.com/images/a5b0c596a130bff78b062749bd75ac77/tenor.gif?itemid=10002831",
                "https://media1.tenor.com/images/a00971f1d12be1540029266530a8c1b7/tenor.gif?itemid=7480906",
                "https://media1.tenor.com/images/a9866434a3ddd2a860048239a70b0e06/tenor.gif?itemid=14775808",
                "https://media1.tenor.com/images/9b0c6e525b2e90b0ed50e2758db684d5/tenor.gif?itemid=9434542",
                "https://media1.tenor.com/images/9e520f7d32a8b6cb5449c295db4e2e5f/tenor.gif?itemid=4884862",
                "https://media1.tenor.com/images/433574e8f96e90d4370fa0f618cde814/tenor.gif?itemid=10216474",
                "https://media1.tenor.com/images/8dee438ec0ee975375b7bca7efb47e11/tenor.gif?itemid=4607781",
                "https://media1.tenor.com/images/22568c7293f1a8375392d86b47186355/tenor.gif?itemid=9181485",
                "https://media1.tenor.com/images/f8a659c13eaba712708e8b9797236e3c/tenor.gif?itemid=13281452",
                "https://media1.tenor.com/images/1f75d734bfe69aa3d468a0defc6c4b5a/tenor.gif?itemid=9278201",
                "https://media1.tenor.com/images/6ed656e93d34f089d1c54366d9ca4f81/tenor.gif?itemid=7380119",
                "https://media1.tenor.com/images/2cdc20be64a750201b23a72cd3e19e6b/tenor.gif?itemid=9214216",
                "https://media1.tenor.com/images/e194c072efada4a3dd96ed44cbda7d10/tenor.gif?itemid=10810504"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
