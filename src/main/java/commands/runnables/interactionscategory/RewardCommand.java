package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "reward",
        emoji = "\uD83C\uDF53",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "rewards" }
)
public class RewardCommand extends RolePlayAbstract {

    public RewardCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736276242466078761/736276249969950781/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568281279336448/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568422241529946/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568536582422588/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568693650743376/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568778174361610/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568918062788678/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885569060773961880/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885569140675452958/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885569245600153660/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885569328454455346/reward.gif"
        );
    }

}
