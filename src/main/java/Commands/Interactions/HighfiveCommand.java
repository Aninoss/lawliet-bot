package Commands.Interactions;
import CommandListeners.onRecievedListener;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class HighfiveCommand extends InteractionCommand implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public HighfiveCommand() {
        super();
        trigger = "highfive";
        emoji = "✋";
        nsfw = false;
        gifs = new String[]{
                "https://media1.tenor.com/images/16267f3a34efb42598bd822effaccd11/tenor.gif?itemid=14137081",
                "https://media1.tenor.com/images/0c23b320822afd5b1ce3faf01c2b9b69/tenor.gif?itemid=14137078",
                "https://media1.tenor.com/images/ce85a2843f52309b85515f56a0a49d06/tenor.gif?itemid=14137077",
                "https://media1.tenor.com/images/b714d7680f8b49d69b07bc2f1e052e72/tenor.gif?itemid=13400356",
                "https://media1.tenor.com/images/57134514aa6358f2d01266f95754a70f/tenor.gif?itemid=10150970",
                "https://media1.tenor.com/images/7b1f06eac73c36721912edcaacddf666/tenor.gif?itemid=10559431",
                "https://media1.tenor.com/images/d9789c904472970f6654633ac2b03aa1/tenor.gif?itemid=4746486",
                "https://media1.tenor.com/images/9730876547cb3939388cf79b8a641da9/tenor.gif?itemid=8073516",
                "https://media1.tenor.com/images/c3263b8196afc25ddc1d53a4224347cd/tenor.gif?itemid=9443275",
                "https://media1.tenor.com/images/993dd262cea4648906251bf93d28f86f/tenor.gif?itemid=4810616",
                "https://media1.tenor.com/images/d0c94badcb39bc04e3b511193a48ad67/tenor.gif?itemid=4923201",
                "https://media1.tenor.com/images/0b8c573239f032bb3156d35b0d5865d2/tenor.gif?itemid=13354957",
                "https://media1.tenor.com/images/56d6725009312574e4798c732cebc5fe/tenor.gif?itemid=12312526",
                "https://media1.tenor.com/images/7d01627fbc6236659aea48d965e88956/tenor.gif?itemid=10216947",
                "https://media1.tenor.com/images/b24854d8f00780c1c3920868e74a4946/tenor.gif?itemid=5374002",
                "https://media1.tenor.com/images/106c8e64e864230341b59cc892b5a980/tenor.gif?itemid=5682921",
                "https://media1.tenor.com/images/e96d2396570a2fadd9c83e284a1ca675/tenor.gif?itemid=5390726",
                "https://media1.tenor.com/images/e2f299d05a7b1832314ec7a331440d4e/tenor.gif?itemid=5374033",
                "https://media1.tenor.com/images/8cd77aed760dd7a0d5209f183e1fdf4e/tenor.gif?itemid=4318510"

        };
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return onInteractionRecieved(event, followedString, picked);
    }
}
