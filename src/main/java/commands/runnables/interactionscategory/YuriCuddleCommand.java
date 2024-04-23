package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yuricuddle",
        emoji = "\uD83D\uDC69\uD83D\uDC50\uD83D\uDC69",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "yurisnuggle" }
)
public class YuriCuddleCommand extends RolePlayAbstract {

    public YuriCuddleCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736280073379774536/736280076546474075/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280078975238154/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280083110690836/yuricuddle.gif",
                "https://cdn.discordapp.com/attachments/736280073379774536/736280085903966339/yuricuddle.gif",
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
    }

}
