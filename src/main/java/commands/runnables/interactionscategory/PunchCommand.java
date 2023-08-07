package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "punch",
        emoji = "\uD83D\uDC4A",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "hit", "attack" }
)
public class PunchCommand extends RolePlayAbstract {

    public PunchCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736270998772383826/736271005756031066/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271012462723223/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271042212790339/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271047208468490/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271054988771408/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271060869316728/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271072332349511/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271075901440060/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271083208179743/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271086987116624/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271092628324392/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271099179958282/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271107866493009/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271117974634566/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271123175702581/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271128678498454/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271140875665408/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271157933637652/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271178955489352/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271189055504394/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271197766942750/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271205262426172/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271229358571550/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271238405554186/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271248845439077/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271259679064205/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271270789775481/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271275680465028/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271418265829436/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271285230764152/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271295062343740/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/736271300414144552/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/834837166047625256/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/834837177132646490/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/834837214512283738/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/834837230161100870/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/834837245462183936/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/834837260552896612/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/834837274155155546/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/871035786491985950/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/890291777133772840/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/925805711265497088/punch.gif",
                "https://cdn.discordapp.com/attachments/736270998772383826/1138103151933788180/punch.gif"
        );
    }

}
