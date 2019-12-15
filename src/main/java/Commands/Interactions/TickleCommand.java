package Commands.Interactions;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "tickle",
        emoji = "\uD83E\uDD23",
        executable = false
)
public class TickleCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public TickleCommand() {
        super("https://media1.tenor.com/images/fea79fed0168efcaf1ddfb14d8af1a6d/tenor.gif?itemid=7283507",
                "https://media1.tenor.com/images/fcbded4ce66ab01317ee009a1aa44404/tenor.gif?itemid=11920137",
                "https://media1.tenor.com/images/f43da23b4ed0938ce362b0374b88e42c/tenor.gif?itemid=8054679",
                "https://media1.tenor.com/images/d38554c6e23b86c81f8d4a3764b38912/tenor.gif?itemid=11379131",
                "https://media1.tenor.com/images/16662667791fc3275c25db595fdf89f8/tenor.gif?itemid=12374065",
                "https://media1.tenor.com/images/b1fc22bec9d5e8fed0c2269ef14824bf/tenor.gif?itemid=5235178",
                "https://media1.tenor.com/images/02f62186ccb7fa8a2667f3216cfd7e13/tenor.gif?itemid=13269751",
                "https://media1.tenor.com/images/e74babbf6fdcbd8189d18bb5b9379bc7/tenor.gif?itemid=12693902",
                "https://media1.tenor.com/images/b444381e7e6d7db202b223c9e584d684/tenor.gif?itemid=12338731",
                "https://media1.tenor.com/images/05a64a05e5501be2b1a5a734998ad2b2/tenor.gif?itemid=11379130"
        );
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
