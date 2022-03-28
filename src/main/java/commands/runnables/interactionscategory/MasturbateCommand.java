package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "masturbate",
        emoji = "✊",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = { "fap", "jerkoff" }
)
public class MasturbateCommand extends RolePlayAbstract {

    public MasturbateCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/834831425651474442/834831559366279178/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831584216481802/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831608800215060/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831631893135380/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831683081207919/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831694942044260/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831705964544071/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831745881997382/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831757462208613/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831769156845608/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831780376477816/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831790748467220/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831804020162660/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831815612825643/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/834831827457802271/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/881905216432259072/masturbate.gif",
                "https://cdn.discordapp.com/attachments/834831425651474442/958006810047045662/masturbate.gif"
        );
    }

}
