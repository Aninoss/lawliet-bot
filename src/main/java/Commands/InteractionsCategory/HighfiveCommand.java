package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "highfive",
        emoji = "✋",
        executable = false
)
public class HighfiveCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/16267f3a34efb42598bd822effaccd11/tenor.gif?itemid=14137081",
                "https://media1.tenor.com/images/0c23b320822afd5b1ce3faf01c2b9b69/tenor.gif?itemid=14137078",
                "https://media1.tenor.com/images/ce85a2843f52309b85515f56a0a49d06/tenor.gif?itemid=14137077",
                "https://media1.tenor.com/images/7b1f06eac73c36721912edcaacddf666/tenor.gif?itemid=10559431",
                "https://media1.tenor.com/images/d9789c904472970f6654633ac2b03aa1/tenor.gif?itemid=4746486",
                "https://media1.tenor.com/images/c3263b8196afc25ddc1d53a4224347cd/tenor.gif?itemid=9443275",
                "https://media1.tenor.com/images/993dd262cea4648906251bf93d28f86f/tenor.gif?itemid=4810616",
                "https://media1.tenor.com/images/d0c94badcb39bc04e3b511193a48ad67/tenor.gif?itemid=4923201",
                "https://media1.tenor.com/images/0b8c573239f032bb3156d35b0d5865d2/tenor.gif?itemid=13354957",
                "https://media1.tenor.com/images/56d6725009312574e4798c732cebc5fe/tenor.gif?itemid=12312526",
                "https://media1.tenor.com/images/b24854d8f00780c1c3920868e74a4946/tenor.gif?itemid=5374002",
                "https://media1.tenor.com/images/106c8e64e864230341b59cc892b5a980/tenor.gif?itemid=5682921",
                "https://media1.tenor.com/images/e96d2396570a2fadd9c83e284a1ca675/tenor.gif?itemid=5390726",
                "https://media1.tenor.com/images/e2f299d05a7b1832314ec7a331440d4e/tenor.gif?itemid=5374033",
                "https://media1.tenor.com/images/8cd77aed760dd7a0d5209f183e1fdf4e/tenor.gif?itemid=4318510",
                "https://media1.tenor.com/images/16267f3a34efb42598bd822effaccd11/tenor.gif?itemid=14137081"
        };
    }

}
