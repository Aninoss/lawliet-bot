package commands.runnables.nsfwinteractionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "spank",
        emoji = "\uD83C\uDF51",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        nsfw = true
)
public class SpankCommand extends RolePlayAbstract {

    public SpankCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true,
                "https://cdn.discordapp.com/attachments/736274084488282113/736274092105269308/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274104361025606/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274108337225818/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274115195043931/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274118776979483/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274121486237736/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274125311443056/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274130747523072/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274138632552478/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274143187828757/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274153820127362/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/736274160266903674/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842062976188456/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842075178467389/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842096350789692/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842123806965790/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842136088150016/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842148817862656/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842161659904100/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842176428703824/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842192396288040/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842212650844160/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842232880234516/spank.gif",
                "https://cdn.discordapp.com/attachments/736274084488282113/834842252601065562/spank.gif"
        );
    }

}
