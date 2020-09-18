package commands.runnables.interactionscategory;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "reward",
        emoji = "\uD83C\uDF53",
        executable = true,
        aliases = { "rewards" }
)
public class RewardCommand extends InteractionAbstract {

    public RewardCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736276242466078761/736276249969950781/reward.gif"
        };
    }

}
