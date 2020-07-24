package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

@CommandProperties(
        trigger = "reward",
        emoji = "\uD83C\uDF53",
        executable = true,
        aliases = { "rewards" }
)
public class RewardCommand extends InteractionAbstract {

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736276242466078761/736276249969950781/reward.gif"
        };
    }

}
