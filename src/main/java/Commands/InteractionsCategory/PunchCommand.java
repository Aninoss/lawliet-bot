package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;

import Commands.InteractionAbstract;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "punch",
        emoji = "\uD83D\uDC4A",
        executable = false,
        aliases = {"hit", "attack"}
)
public class PunchCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/31686440e805309d34e94219e4bedac1/tenor.gif?itemid=4790446",
                "https://media1.tenor.com/images/2487a7679b3d7d23cadcd51381635467/tenor.gif?itemid=11451829",
                "https://media1.tenor.com/images/c621075def6ca41785ef4aaea20cc3a2/tenor.gif?itemid=7679409",
                "https://media1.tenor.com/images/79cc6480652032a20f1cb5c446b113ae/tenor.gif?itemid=12911685",
                "https://media1.tenor.com/images/cff010b188084e1faed2905c0f1634c2/tenor.gif?itemid=10161883",
                "https://media1.tenor.com/images/ee3f2a6939a68df9563a7374f131fd96/tenor.gif?itemid=14210784",
                "https://media1.tenor.com/images/965fabbfcdc09ee0eb4d697e25509f34/tenor.gif?itemid=7380310",
                "https://media1.tenor.com/images/d7c30e46a937aaade4d7bc20eb09339b/tenor.gif?itemid=12003970",
                "https://media1.tenor.com/images/6afcfbc435b698fa5ceb2ff019718e6d/tenor.gif?itemid=10480971",
                "https://media1.tenor.com/images/1c986c555ed0b645670596d978c88f6e/tenor.gif?itemid=13142581",
                "https://media1.tenor.com/images/2fe2e31bd486f36dd552f4d6e2e5b602/tenor.gif?itemid=10606507",
                "https://media1.tenor.com/images/5511a8309a1719987a27aa7b1ee7da04/tenor.gif?itemid=12303482",
                "https://media1.tenor.com/images/f03329d8877abfde62b1e056953724f3/tenor.gif?itemid=13785833",
                "https://media1.tenor.com/images/7d43687195b86c8ce2411484eb1951fc/tenor.gif?itemid=7922533",
                "https://media1.tenor.com/images/517f63ce5ffc6426bddd17d7413ebaca/tenor.gif?itemid=5247335",
                "https://media1.tenor.com/images/2efcac044a4f9f2377b118b1cc6282cb/tenor.gif?itemid=13142576",
                "https://media1.tenor.com/images/0dbb53b0f2a8730ea3c8a0e2502b6bac/tenor.gif?itemid=10194762",
                "https://media1.tenor.com/images/745d16a823805edbfe83aa9363c48a87/tenor.gif?itemid=12003981",
                "https://media1.tenor.com/images/b2308e16fa5b71c541efdd13dea4f9ba/tenor.gif?itemid=10462739",
                "https://media1.tenor.com/images/2c96a0f0c2e7f5e446b6771fe1b6fca8/tenor.gif?itemid=14949242",
                "https://i.imgur.com/9wi47g3.gif",
                "https://data.whicdn.com/images/314492571/original.gif",
                "https://media1.tenor.com/images/2e36b49b3d26d1e2fe014e5d4c1dbc75/tenor.gif?itemid=15580060",
                "https://media1.tenor.com/images/16c587440a4f8301c22ed04625e0f868/tenor.gif?itemid=16922805",
                "https://media1.tenor.com/images/f1294897c63a4e82da1f466dc3764b2d/tenor.gif",
                "https://media1.tenor.com/images/e72966a32c66380aa288009cb0379b32/tenor.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/708657611842650152/anime-punch-gif-3.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/708657620017348628/giphy.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/708657624320835586/ImperfectFrightenedFoal-size_restricted.gif",
                "https://cdn.discordapp.com/attachments/499629904380297226/711608276105691196/Black_Clover_Lumiere_Punch.gif"
        };
    }

}
