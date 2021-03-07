package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "hug",
        emoji = "\uD83D\uDC50",
        executableWithoutArgs = true
)
public class HugCommand extends InteractionAbstract {

    public HugCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736277561373491265/736277568629506146/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277578633183293/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277583767011368/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277588615626813/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277592553816174/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277597377265674/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277600053493770/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277606994804866/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277610958684220/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277617833017364/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277628217983087/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277637873270935/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277642160111717/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277645699973190/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277652653998110/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277657896878121/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277665115275465/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277668835885076/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277671260061846/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277680328015882/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277685034287213/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277688381341797/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277693779279913/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277706383032339/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277717816836197/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277721272942702/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277724397830184/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277727979634718/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277731561570848/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277740558221332/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277746157617152/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277749949268048/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277752080236604/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277760049152040/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277769209774190/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277774616100954/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277779372572783/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277783218749510/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277786779451483/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277791644975104/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277796568957000/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277800251818154/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277804412436567/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277813442642060/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277819969110097/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277834343120996/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277840466542632/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277845776793771/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277851216806057/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277857684160583/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277861974933584/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277871139618917/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277876034371594/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277881524715560/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277952894861352/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736277965591281684/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736278192850993192/hug.gif",
                "https://media.discordapp.net/attachments/736277561373491265/736278197426978907/hug.gif"
        };
    }

}
