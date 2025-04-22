package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "laugh",
        emoji = "\uD83D\uDE06",
        executableWithoutArgs = true,
        aliases = { "lol", "funny" }
)
public class LaughCommand extends RolePlayAbstract {

    public LaughCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736261908960903268/736261913096224869/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261919228297256/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261923296903269/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261940090765382/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261959070122094/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261963457232906/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261968339533904/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261978367983646/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261994600202327/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262019417899059/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262024958574613/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262034429050952/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262047733514260/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262058273800212/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262065659969586/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262072521851050/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262097582948422/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262105078169690/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262112862666873/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262118155747370/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262133586591875/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262137651003502/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262151227965477/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262180717985802/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262199814652005/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512083491225680/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512120082333728/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512145076191312/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512158397693982/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512220519923742/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512234242637854/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512248181489674/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512262147604510/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512341450096670/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/839473363210600448/laugh.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736261908960903268/736261916850126968/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261929936617522/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261943282892860/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261971959349329/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261990053576804/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736261998060372110/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262004003700876/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262016590676018/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262028955746334/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262040833753088/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262053068669009/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262091312201840/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262155380326566/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262160983916604/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262167606591698/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262210837413961/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262227014713476/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/736262326545547284/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512073001271297/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512095142871150/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512107038310440/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512132301258772/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512170825023578/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512181780807691/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512194368438292/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512207441035264/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512277126250567/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512291705389056/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512306444173322/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512317311090748/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/834512352736182352/laugh.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736261908960903268/736262141799170140/laugh.gif",
                "https://cdn.discordapp.com/attachments/736261908960903268/881898218953252894/laugh.gif"
        );
    }

}
