package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "boobsuck",
        emoji = "🍒",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = { "boobssuck" ,"boobysuck", "suckboob", "suckboobs" }
)
public class BoobSuckCommand extends RolePlayAbstract {

    public BoobSuckCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtfGifs(
                "https://cdn.discordapp.com/attachments/965983252718428230/965983462442008606/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985092562473050/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985320116027472/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985699201425429/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985737621266462/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985802683310220/boobsuck.gif"
        );
        setMtfGifs(
                "https://cdn.discordapp.com/attachments/965983252718428230/965983495983866007/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965983534370140211/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985028876169226/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985344984064000/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985776829628496/boobsuck.gif",
                "https://cdn.discordapp.com/attachments/965983252718428230/965985839802904626/boobsuck.gif"
        );
    }

}
