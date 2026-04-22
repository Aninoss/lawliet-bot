package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

import static commands.runnables.informationcategory.HelpCommand.RP_SUBCATEGORY_INTERACTIVE;

@CommandProperties(
        trigger = "handjob",
        emoji = "✊",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        subCategory = RP_SUBCATEGORY_INTERACTIVE
)
public class HandjobCommand extends RolePlayAbstract {

    public HandjobCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtmGifs(
                "https://cdn.discordapp.com/attachments/972916898436108318/972917081148391454/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972917114388238427/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972917143890952322/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972918667597721740/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972918776725114890/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972918826708652102/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972918853464121344/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972919151293235340/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972919627866832946/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/972919659026341989/handjob.gif",
                "https://cdn.discordapp.com/attachments/972916898436108318/1130879599786594414/handjob.gif"
        );
    }

}
