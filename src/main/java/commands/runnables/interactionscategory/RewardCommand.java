package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "reward",
        emoji = "\uD83C\uDF53",
        executableWithoutArgs = true,
        requiresMemberCache = true,
        aliases = { "rewards" }
)
public class RewardCommand extends RolePlayAbstract {

    public RewardCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736276242466078761/736276249969950781/reward.gif"
        );
    }

}
