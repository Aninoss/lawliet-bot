package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "dance",
        emoji = "\uD83D\uDD7A",
        executableWithoutArgs = true,
        aliases = { "party" }
)
public class DanceCommand extends RolePlayAbstract {

    public DanceCommand(Locale locale, String prefix) {
        super(locale, prefix, false,
                "https://cdn.discordapp.com/attachments/736255695296528484/736255699134316634/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255702888218645/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255712967131146/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255716825890936/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255721024520243/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255733183676506/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255736828395571/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255741056253993/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255748463525958/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255753236643890/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255762107596810/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255767702798447/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255770919829575/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255778809446510/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255781896192040/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255785176399972/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255798123954337/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255805346676797/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255811684401274/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255818269458452/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255825542119504/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255833431867472/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736256109261881344/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/736255845733498900/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/745278813138583702/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/745279078990348308/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/750018053256904806/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/758372718806171788/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/781298738953715722/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/816316872294137916/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/816323356437381160/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834502905369329735/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834502917180227614/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834502929511612516/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834502941398269972/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834502966258040882/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834502978777645136/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834502991176138762/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503002319880222/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503014348750938/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503026709626890/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503040467206154/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503054858387526/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503068090630244/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503081827237888/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503095156736030/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503108851793970/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503122587746324/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503136894648450/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503153172086836/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503169378877530/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503187494469652/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503203189293166/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503218691047464/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503234604892190/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503247385460746/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503258885062716/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503271569031248/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503287327162418/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503299255762944/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503309963427860/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503323096317952/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503337239642125/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503348740816936/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503359733039214/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503371116642364/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503382953623566/dance.gif",
                "https://cdn.discordapp.com/attachments/736255695296528484/834503395229302784/dance.gif"
        );
    }

}
