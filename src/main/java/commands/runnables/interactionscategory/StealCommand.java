package commands.runnables.interactionscategory;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "steal",
        emoji = "❔",
        executableWithoutArgs = true
)
public class StealCommand extends InteractionAbstract {

    public StealCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736271960065048577/736271967300223079/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271972966858772/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271981372112958/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271984941596682/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271990607970424/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736271997977362572/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736272008761049199/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736272017590059038/steal.gif",
                "https://media.discordapp.net/attachments/736271960065048577/736272026041450557/steal.gif"
        };
    }

}
