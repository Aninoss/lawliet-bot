package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "nosebleed",
        emoji = "\uD83E\uDE78",
        executableWithoutArgs = true
)
public class NoseBleedCommand extends RolePlayAbstract {

    public NoseBleedCommand(Locale locale, String prefix) {
        super(locale, prefix, false,
                "https://cdn.discordapp.com/attachments/736261590508371980/736261596900491375/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261602525053009/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261608615182476/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261613270597632/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261620728070174/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261627740946452/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261637320736838/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261644497190962/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261646690811965/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261652332281927/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261662092558406/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261667767451768/nosebleed.gif",
                "https://cdn.discordapp.com/attachments/736261590508371980/736261672938897408/nosebleed.gif"
        );
    }

}
