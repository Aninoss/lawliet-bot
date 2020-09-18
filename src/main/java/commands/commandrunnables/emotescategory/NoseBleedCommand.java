package commands.commandrunnables.emotescategory;
import commands.commandlisteners.CommandProperties;
import commands.commandrunnables.EmoteAbstract;

import java.util.Locale;

@CommandProperties(
    trigger = "nosebleed",
    emoji = "\uD83E\uDE78",
    executable = true
)
public class NoseBleedCommand extends EmoteAbstract {

    public NoseBleedCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736261590508371980/736261596900491375/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261602525053009/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261608615182476/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261613270597632/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261620728070174/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261627740946452/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261637320736838/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261644497190962/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261646690811965/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261652332281927/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261662092558406/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261667767451768/nosebleed.gif",
                "https://media.discordapp.net/attachments/736261590508371980/736261672938897408/nosebleed.gif"
        };
    }

}
