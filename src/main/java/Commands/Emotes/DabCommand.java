package Commands.Emotes;
import CommandListeners.onRecievedListener;
import Commands.Interactions.InteractionCommand;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class DabCommand extends EmoteCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public DabCommand() {
        super();
        trigger = "dab";
        emoji = "\uD83C\uDD92";
        nsfw = false;
        gifs = new String[]{
                "https://media1.tenor.com/images/d13c16a8853e3b309db0ec7e573c4c94/tenor.gif?itemid=10617637",
                "https://media1.tenor.com/images/033a2e811f625be20008eed617734d40/tenor.gif?itemid=13628207",
                "https://media1.tenor.com/images/af1d454ec3432222fc82af6eab154869/tenor.gif?itemid=13945012",
                "https://media1.tenor.com/images/dddd0236ad6d889e54df034f18383871/tenor.gif?itemid=5774497",
                "https://media1.tenor.com/images/425cf503dedae77ca0ffcfebd0ddc5d7/tenor.gif?itemid=12692598",
                "https://media1.tenor.com/images/59983e51f744411fd00c2e50b1399fc5/tenor.gif?itemid=12789689"
        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
