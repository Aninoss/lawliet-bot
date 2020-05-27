package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "highfive",
        emoji = "✋",
        executable = true
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
                "https://media1.tenor.com/images/16267f3a34efb42598bd822effaccd11/tenor.gif?itemid=14137081",
                "https://i.imgur.com/Pr1rEzX.gif",
                "https://i.pinimg.com/originals/a2/27/e0/a227e02b4a4f69a97ed71122d5d0e325.gif",
                "https://gifimage.net/wp-content/uploads/2017/09/anime-high-five-gif-10.gif",
                "https://i.pinimg.com/originals/fc/b1/44/fcb1446b74166b0860ace50ed8b33686.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433716807827556/lls.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433716769947669/Sewayaki_Kitsune_no_Senko-san_-_Episode_12_END_-_Shiro_Koenji_High_Five.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433710432485436/unnamed.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433710193148005/tumblr_n04birgZxv1rl376ro1_500.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433708704432158/giphy.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433707869634600/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f776174747061642d6d656469612d736572766963652f53746f.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433707467112518/highfive.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433705734602862/tenor.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/709433702614171728/tumblr_o0l51tCl4T1tieeldo1_500.gif"
        };
    }

}
