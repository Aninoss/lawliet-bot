package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "stab",
        emoji = "\uD83D\uDD2A️",
        executableWithoutArgs = true,
        aliases = { "knife" }
)
public class StabCommand extends RolePlayAbstract {

    public StabCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/868943819717095476/868944202090827816/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868945209529085993/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868945330174033960/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868945558461636626/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868945689072250880/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868945863995699250/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868946001380126740/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868946158679109692/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868946269756858378/stab.gif",
                "https://cdn.discordapp.com/attachments/868943819717095476/868946404389826620/stab.gif"
        );
    }

}
