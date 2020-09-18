package commands.commandrunnables.emotescategory;
import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.EmoteAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "cry",
        emoji = "\uD83D\uDE2D",
        executable = true,
        aliases = {"sad" , "sob"}
)
public class CryCommand extends EmoteAbstract {

    public CryCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736255198808375307/736255201878474822/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255204030283837/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255209361375263/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255212142198876/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255214612381823/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255228172566568/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255232438304798/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255239023362138/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255243503009902/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255251346227210/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255257121783858/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255261546905740/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255266105851954/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255274045669487/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255279582281748/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255286712467546/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255291976581220/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255295038160937/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255301916950588/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255307071881246/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255314126438400/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255317897248778/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255326398971944/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255330215788714/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255333911101501/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255339728732170/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255344954572841/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255350591717386/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255357063790723/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255361488781342/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255363741122660/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255369969532970/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255377737384067/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255386889486408/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255403574427739/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255409085481140/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255416689885355/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255422050336809/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255433274032192/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255436612960286/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255442233196584/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255448461607022/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255452538470410/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255463347322990/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255472474128444/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255476962033675/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255486961385572/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255492841537546/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255498277355570/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255505856462909/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255509644050543/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255514102595694/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/736255518187978752/cry.gif",
                "https://media.discordapp.net/attachments/736255198808375307/741227824823664650/cry.gif"
        };
    }

}
