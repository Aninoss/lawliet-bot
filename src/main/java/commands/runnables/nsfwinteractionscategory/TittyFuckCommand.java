package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "tittyfuck",
        emoji = "\uD83E\uDD65️",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = { "titjob", "titfuck", "boobfuck", "boobsfuck" }
)
public class TittyFuckCommand extends RolePlayAbstract {

    public TittyFuckCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true);

        setFtmGifs(
                "https://cdn.discordapp.com/attachments/834844814671478824/834844931423993946/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834844958598758421/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834844969951952916/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834844981931409438/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834844998775078952/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845029348409364/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845041263378501/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845053163536404/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845068141133874/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845083684962364/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845094661718016/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845106716540988/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845117496033280/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845130196385832/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845146393739284/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/834845159539081226/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/881905383625596959/tittyfuck.gif",
                "https://cdn.discordapp.com/attachments/834844814671478824/881905488055377960/tittyfuck.gif"
        );
    }

}
