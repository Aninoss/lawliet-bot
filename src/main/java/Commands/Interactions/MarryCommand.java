package Commands.Interactions;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class MarryCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public MarryCommand() {
        super();
        trigger = "marry";
        emoji = "\uD83D\uDC8D";
        nsfw = false;
        gifs = new String[]{
                "https://media1.tenor.com/images/76807e7b666fb954529509e107fff97d/tenor.gif?itemid=10799166",
                "https://media1.tenor.com/images/58bd69fb056bd54b80c92581f3cd9cf9/tenor.gif?itemid=10799169",
                "https://media1.tenor.com/images/f11b1e79d26818b34939b4e0a69a40e0/tenor.gif?itemid=10799170",
                "https://media1.tenor.com/images/a57932afb4c5d2d93b69b496afac1366/tenor.gif?itemid=10799171",
                "https://media1.tenor.com/images/d5725e6384ba532281e0ca8ec9e5db24/tenor.gif?itemid=7507476",
                "https://media1.tenor.com/images/69dbcb02b724d26644228a38e367d017/tenor.gif?itemid=14444888",
                "https://media1.tenor.com/images/4acbe4020146bd1a888ac27f6f07da21/tenor.gif?itemid=7302786",
                "https://media1.tenor.com/images/783e9568a1c06da76a50dc2c98129f11/tenor.gif?itemid=12390162",
                "https://media1.tenor.com/images/f3007ab7d6ba111cde3103840f2a5c52/tenor.gif?itemid=5412366",

        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
