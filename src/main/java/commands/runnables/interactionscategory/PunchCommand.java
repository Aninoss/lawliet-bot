package commands.runnables.interactionscategory;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "punch",
        emoji = "\uD83D\uDC4A",
        executableWithoutArgs = true,
        aliases = {"hit", "attack"}
)
public class PunchCommand extends InteractionAbstract {

    public PunchCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736270998772383826/736271005756031066/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271012462723223/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271018930208818/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271024047521873/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271030565339217/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271042212790339/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271047208468490/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271054988771408/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271060869316728/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271072332349511/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271075901440060/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271083208179743/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271086987116624/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271092628324392/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271099179958282/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271107866493009/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271117974634566/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271123175702581/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271128678498454/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271134676484106/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271140875665408/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271157933637652/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271178955489352/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271189055504394/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271197766942750/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271205262426172/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271229358571550/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271238405554186/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271248845439077/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271259679064205/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271270789775481/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271275680465028/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271418265829436/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271285230764152/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271295062343740/punch.gif",
                "https://media.discordapp.net/attachments/736270998772383826/736271300414144552/punch.gif"
        };
    }

}
