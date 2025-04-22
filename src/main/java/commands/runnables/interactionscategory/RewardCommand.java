package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "reward",
        emoji = "\uD83C\uDF53",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "rewards" }
)
public class RewardCommand extends RolePlayAbstract {

    public RewardCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736276242466078761/893967683048059021/reward.gif"
        );
        setMtmGifs(
                "https://cdn.discordapp.com/attachments/736276242466078761/736276249969950781/reward.gif"
        );
        setAtfGifs(
                "https://cdn.discordapp.com/attachments/736276242466078761/885568422241529946/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568536582422588/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568693650743376/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885568778174361610/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885569060773961880/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885569245600153660/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/885569328454455346/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/893967575573200906/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/893967877269504040/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/893968111945023588/reward.gif"
        );
        setAtmGifs(
                "https://cdn.discordapp.com/attachments/736276242466078761/885568918062788678/reward.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736276242466078761/885569140675452958/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/893967786341203968/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/893967971326763059/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/893968043774996490/reward.gif",
                "https://cdn.discordapp.com/attachments/736276242466078761/991341412861874246/reward.gif"
        );
    }

}
