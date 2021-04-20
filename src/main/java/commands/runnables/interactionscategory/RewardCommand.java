package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

@CommandProperties(
        trigger = "reward",
        emoji = "\uD83C\uDF53",
        executableWithoutArgs = true,
        aliases = { "rewards" }
)
public class RewardCommand extends InteractionAbstract {

    public RewardCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[] {
                "https://cdn.discordapp.com/attachments/736276242466078761/736276249969950781/reward.gif"
        };
    }

}
