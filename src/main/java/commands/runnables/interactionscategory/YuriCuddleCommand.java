package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yuricuddle",
        emoji = "\uD83D\uDC69\uD83D\uDC50\uD83D\uDC69",
        executable = true,
        aliases = {"yurisnuggle"}
)
public class YuriCuddleCommand extends InteractionAbstract {

    public YuriCuddleCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736280073379774536/736280076546474075/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280078975238154/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280083110690836/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280085903966339/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280093290135622/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280099342778498/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280106825416785/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280110692565062/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280113208885268/yuricuddle.gif",
                "https://media.discordapp.net/attachments/736280073379774536/736280178623250568/yuricuddle.gif"
        };
    }

}
