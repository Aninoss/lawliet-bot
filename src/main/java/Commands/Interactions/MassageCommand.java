package Commands.Interactions;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class MassageCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public MassageCommand() {
        super();
        trigger = "massage";
        emoji = "\uD83D\uDC86";
        nsfw = false;
        gifs = new String[]{
                "https://media1.tenor.com/images/71e74263a48a6e9a2c53f3bc1439c3ac/tenor.gif?itemid=12434286",
                "https://media1.tenor.com/images/8d995dfe4679d6d8f4c3c59ec39370c5/tenor.gif?itemid=12822446",
                "https://media1.tenor.com/images/56c06d0f07bfb7c33307c16e1d81e791/tenor.gif?itemid=9214647",
                "https://media1.tenor.com/images/4cbec203d1e480dfd79ec0b5efed742f/tenor.gif?itemid=10166717",
                "https://media1.tenor.com/images/e8825d223bc7f6aae6c31faecd1087da/tenor.gif?itemid=13749329",
                "https://media1.tenor.com/images/d0ebf560496e82f495f4d90570b5221d/tenor.gif?itemid=3542368",
                "https://media1.tenor.com/images/3a4d01bc88cfd87e4c63c7f2b7e726eb/tenor.gif?itemid=3454569"
        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
