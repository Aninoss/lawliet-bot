package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "bully",
        emoji = "\uD83D\uDE08",
        executable = false
)
public class BullyCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public BullyCommand() {
        super( "https://media1.tenor.com/images/cd4e555c2f9e0a269c4bc556c26bad85/tenor.gif?itemid=14116420",
                "https://media1.tenor.com/images/a67eb981bfda1c87e06f80b4cab2b98c/tenor.gif?itemid=5707440",
                "https://media1.tenor.com/images/75d23ea3deef54f1bb0cba73957ede72/tenor.gif?itemid=6132622",
                "https://media1.tenor.com/images/d8eb944ec458745740bf455cc6d50c9f/tenor.gif?itemid=12354084",
                "https://media1.tenor.com/images/608fcf3bb559a51dcea46e6f244a32d8/tenor.gif?itemid=5600466",
                "https://media1.tenor.com/images/0074d757a7b82f0f5860d6847093f715/tenor.gif?itemid=7733341",
                "https://media1.tenor.com/images/8c3eeb259617b1137211ab8f47a68406/tenor.gif?itemid=5661490"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
