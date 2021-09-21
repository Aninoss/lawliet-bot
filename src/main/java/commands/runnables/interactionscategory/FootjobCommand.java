package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "footjob",
        emoji = "🦶",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = { "feetjob", "feet" }
)
public class FootjobCommand extends RolePlayAbstract {

    public FootjobCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/884506240179372112/884506336732278844/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884506467409989642/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884506586196885604/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884506706317570079/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884506781299126302/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884506860911231016/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884507081099608134/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884507187127410708/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884507241447837748/footjob.gif",
                "https://cdn.discordapp.com/attachments/884506240179372112/884507559057317938/footjob.gif"
        );
    }

}
