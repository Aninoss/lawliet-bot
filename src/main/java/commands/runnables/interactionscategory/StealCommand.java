package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "steal",
        emoji = "❔",
        executableWithoutArgs = true,
        requiresMemberCache = true
)
public class StealCommand extends RolePlayAbstract {

    public StealCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736271960065048577/736271967300223079/steal.gif",
                "https://cdn.discordapp.com/attachments/736271960065048577/736271972966858772/steal.gif",
                "https://cdn.discordapp.com/attachments/736271960065048577/736271981372112958/steal.gif",
                "https://cdn.discordapp.com/attachments/736271960065048577/736271984941596682/steal.gif",
                "https://cdn.discordapp.com/attachments/736271960065048577/736271990607970424/steal.gif",
                "https://cdn.discordapp.com/attachments/736271960065048577/736271997977362572/steal.gif",
                "https://cdn.discordapp.com/attachments/736271960065048577/736272008761049199/steal.gif",
                "https://cdn.discordapp.com/attachments/736271960065048577/736272017590059038/steal.gif",
                "https://cdn.discordapp.com/attachments/736271960065048577/736272026041450557/steal.gif"
        );
    }

}
