package Commands.InteractionsCategory;

import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
        trigger = "yurihug",
        emoji = "\uD83D\uDC69\uD83D\uDC50\uD83D\uDC69",
        executable = false
)
public class YuriHugCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media.giphy.com/media/xJlOdEYy0r7ZS/giphy.gif",
                "https://media.giphy.com/media/3EJsCqoEiq6n6/giphy.gif",
                "https://media.giphy.com/media/3og0ILx8f9adnoQRos/giphy.gif",
                "https://media.giphy.com/media/vVA8U5NnXpMXK/giphy.gif",
                "https://media.giphy.com/media/1434tCcpb5B7EI/giphy.gif",
                "https://media.giphy.com/media/aD1fI3UUWC4/giphy.gif",
                "https://static.zerochan.net/Neko.Para.full.2841283.gif",
                "https://static.zerochan.net/Fruits.Basket.full.2657882.gif",
                "https://static.zerochan.net/Sword.Art.Online%3A.Alicization.-War.Of.Underworld-.full.2725165.gif",
                "https://static.zerochan.net/Kannazuki.no.Miko.full.2492522.gif",
                "https://static.zerochan.net/Kannazuki.no.Miko.full.2492729.gif",
                "https://static.zerochan.net/Kannazuki.no.Miko.full.2492532.gif",
                "https://static.zerochan.net/Kannazuki.no.Miko.full.2492728.gif",
                "https://static.zerochan.net/Kannazuki.no.Miko.full.2492720.gif"
        };
    }

}
