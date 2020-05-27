package Commands.InteractionsCategory;

import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
        trigger = "yuricuddle",
        emoji = "\uD83D\uDC69\uD83D\uDC50\uD83D\uDC69",
        executable = true,
        aliases = {"yurisnuggle"}
)
public class YuriCuddleCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media.giphy.com/media/NTBFwChJg9lKM/giphy.gif",
                "https://media.giphy.com/media/3bqtLDeiDtwhq/giphy.gif",
                "https://media.giphy.com/media/PHZ7v9tfQu0o0/giphy.gif",
                "https://media.giphy.com/media/wSY4wcrHnB0CA/giphy.gif",
                "https://media1.tenor.com/images/cb7bad4df6020d733005e5eb164c7979/tenor.gif?itemid=11913032",
                "https://media1.tenor.com/images/71708c254db078de06d3bfa26b8fe37e/tenor.gif?itemid=5085155",
                "https://media1.tenor.com/images/8f8ba3baeecdf28f3e0fa7d4ce1a8586/tenor.gif?itemid=12668750",
                "https://media.giphy.com/media/RLW8eXPdTXs3H1O3gu/giphy.gif",
                "https://media.giphy.com/media/3o7btXOP8qnV4X7nEY/giphy.gif",
                "https://media.giphy.com/media/u9BxQbM5bxvwY/giphy.gif",
                "https://static.zerochan.net/Kannazuki.no.Miko.full.2492728.gif"
        };
    }

}
