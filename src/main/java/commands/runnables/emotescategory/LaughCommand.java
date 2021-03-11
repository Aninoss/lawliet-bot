package commands.runnables.emotescategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.EmoteAbstract;

@CommandProperties(
        trigger = "laugh",
        emoji = "\uD83D\uDE06",
        executableWithoutArgs = true,
        aliases = { "lol", "funny" }
)
public class LaughCommand extends EmoteAbstract {

    public LaughCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://media.discordapp.net/attachments/736261908960903268/736261913096224869/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261916850126968/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261919228297256/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261923296903269/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261929936617522/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261940090765382/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261943282892860/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261959070122094/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261963457232906/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261968339533904/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261971959349329/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261978367983646/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261990053576804/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261994600202327/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736261998060372110/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262004003700876/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262016590676018/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262019417899059/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262024958574613/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262028955746334/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262034429050952/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262040833753088/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262047733514260/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262053068669009/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262058273800212/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262065659969586/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262072521851050/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262091312201840/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262097582948422/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262105078169690/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262112862666873/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262118155747370/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262133586591875/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262137651003502/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262141799170140/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262151227965477/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262155380326566/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262160983916604/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262167606591698/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262180717985802/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262199814652005/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262210837413961/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262227014713476/laugh.gif",
                "https://media.discordapp.net/attachments/736261908960903268/736262326545547284/laugh.gif"
        };
    }

}
