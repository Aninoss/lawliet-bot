package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "cuddle",
        emoji = "\uD83D\uDC50",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "snuggle", "yaoicuddle", "yuricuddle" }
)
public class CuddleCommand extends RolePlayAbstract {

    public CuddleCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/736279549121396816/736279659590844436/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279681246167130/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279695058010112/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279753585328218/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279766386212954/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/834499908903501864/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/834499958156689439/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280076546474075/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280078975238154/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280083110690836/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280093290135622/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280099342778498/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280106825416785/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280110692565062/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280113208885268/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280178623250568/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/829471458212052992/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/834849068630671450/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/834849079427203112/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/1232306116994793503/yuricuddle.gif"
        );
        setFtmGifs(
                "https://cdn.discordapp.com/attachments/736279549121396816/736279565739229275/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279573867659394/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279582050877530/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279596345065572/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279602934317187/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279609166790757/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279617392083005/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279643195310090/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279668340162660/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279713752023392/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279725562921011/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279744655523850/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279758425554984/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279761495785512/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279770983301120/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/834499946576085023/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/839471387680440340/cuddle.gif"
        );
        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736279549121396816/736279634341003335/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279649851539532/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279735188848672/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/834499922052644914/cuddle.gif"
        );
        setMtmGifs(
                "https://cdn.discordapp.com/attachments/736279874452455454/736279883776262144/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279889224925214/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279896518557787/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279903036506202/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279906564178000/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279911534428271/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279914172645556/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279917976617020/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279927313268786/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279930203013270/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279935152554044/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/736279946741153882/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/912101261183160351/yaoicuddle.gif",
                "https://cdn.discordapp.com/attachments/736279874452455454/912101389449166868/yaoicuddle.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736279549121396816/736279622240567447/cuddle.gif",
                "https://cdn.discordapp.com/attachments/736279549121396816/736279685998182420/cuddle.gif"
        );
    }

}
