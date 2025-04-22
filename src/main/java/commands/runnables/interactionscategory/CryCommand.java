package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "cry",
        emoji = "\uD83D\uDE2D",
        executableWithoutArgs = true,
        aliases = { "sad", "sob" }
)
public class CryCommand extends RolePlayAbstract {

    public CryCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736255198808375307/736255201878474822/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255204030283837/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255209361375263/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255212142198876/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255214612381823/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255228172566568/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255232438304798/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255239023362138/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255243503009902/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255257121783858/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255261546905740/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255279582281748/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255286712467546/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255291976581220/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255295038160937/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255301916950588/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255314126438400/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255317897248778/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255326398971944/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255330215788714/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255333911101501/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255344954572841/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255350591717386/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255357063790723/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255363741122660/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255377737384067/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255386889486408/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255403574427739/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255416689885355/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255422050336809/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255442233196584/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255448461607022/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255472474128444/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255476962033675/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255486961385572/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255498277355570/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255505856462909/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255514102595694/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499171133816843/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499230135353384/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499242089381928/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499267179577394/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499333281546240/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499347046596618/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499375421587476/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499419737948160/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499431994097674/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499447861018654/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499460058841128/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499472363880518/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499498419814430/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499510604136498/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499535790145546/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499550445699072/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499563993300992/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499591880572958/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499603398262815/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499614153506896/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499627687477248/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499639411605554/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/838516530265980968/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/881893079093768222/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/881893227391778876/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/881893404198436906/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/1295028454127042600/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/1295028547970142332/cry.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736255198808375307/736255251346227210/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255266105851954/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255274045669487/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255339728732170/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255369969532970/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255433274032192/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255452538470410/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255463347322990/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255492841537546/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/736255509644050543/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/741227824823664650/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/816323891181256744/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499184086351962/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499206445400074/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499218130993192/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499254650798101/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499279594586132/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499292097151020/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499305213395014/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499364067999754/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499389393076264/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499404836634684/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499485261758484/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499522175697036/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499576785535067/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/839471288501665852/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/881893675502821396/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/1295028060512587817/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/1295028352675090463/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/1295029281231208499/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/1295029874100273152/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/1295030071123509278/cry.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736255198808375307/736255436612960286/cry.gif",
                "https://cdn.discordapp.com/attachments/736255198808375307/834499195162984478/cry.gif"
        );
    }

}
