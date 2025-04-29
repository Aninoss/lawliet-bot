package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "hug",
        emoji = "👐",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "yaoihug", "yurihug" }
)
public class HugCommand extends RolePlayAbstract {

    public HugCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/736277561373491265/736277583767011368/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277606994804866/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277610958684220/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277652653998110/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277657896878121/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277668835885076/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277688381341797/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277721272942702/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277760049152040/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277769209774190/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277779372572783/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277834343120996/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277965591281684/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508606530781245/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508715729485874/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/840657468051816510/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/1011397991594209380/hug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278809795625080/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278814065426573/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278820256088094/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278846114103316/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278854955565066/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278911939379345/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736278978184347759/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736279020110479480/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736279287572725821/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736279360092373102/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/736279429419892776/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/834849513579216956/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/834849563474657320/yurihug.gif",
                "https://cdn.discordapp.com/attachments/736278804829306930/834849577344434196/yurihug.gif"
        );
        setFtmGifs(
                "https://cdn.discordapp.com/attachments/736277561373491265/736277568629506146/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277578633183293/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277588615626813/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277592553816174/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277597377265674/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277600053493770/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277617833017364/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277628217983087/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277637873270935/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277645699973190/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277665115275465/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277680328015882/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277685034287213/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277693779279913/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277706383032339/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277717816836197/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277727979634718/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277749949268048/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277752080236604/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277774616100954/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277783218749510/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277791644975104/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277800251818154/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277813442642060/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277819969110097/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277845776793771/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277851216806057/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277857684160583/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277861974933584/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277881524715560/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277952894861352/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508552797814815/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508565360279592/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508579141976114/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508592752623616/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508619792908378/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508630862725190/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508688806772746/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508727704617000/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508740031938617/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508753704321044/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508768305872907/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508782738079814/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508794733920266/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508808033665034/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508820084293652/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508831765430342/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/871035982810603540/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/881895870746996766/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/881895989374488617/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/881896230525992991/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/893594251961700372/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/938231448211697684/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/1012359377849679892/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/1046370316210937928/hug.gif"
        );
        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736277561373491265/736277671260061846/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277724397830184/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277796568957000/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277871139618917/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736278192850993192/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508646049644574/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508661291352114/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508675119841320/hug.gif"
        );
        setMtmGifs(
                "https://cdn.discordapp.com/attachments/736277561373491265/736277731561570848/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277804412436567/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277876034371594/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736278197426978907/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/834508700487516210/hug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278344986918954/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278350959607949/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278355015630868/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278366818271302/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278374993100840/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278379849973780/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278403434676224/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278410568925315/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278429510533176/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/736278434866790532/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/741227684587241503/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/834717466424901652/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/877645631051665519/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/881896699277238282/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/1150478633740271687/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/1334543487915524247/yaoihug.gif",
                "https://cdn.discordapp.com/attachments/736278340733894710/1366354345246855258/yaoihug.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736277561373491265/736277786779451483/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/736277840466542632/hug.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736277561373491265/736277746157617152/hug.gif",
                "https://cdn.discordapp.com/attachments/736277561373491265/904068109302915072/hug.gif"
        );
    }

}
