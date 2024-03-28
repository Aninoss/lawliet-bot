package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "bathroomfuck",
        emoji = "🛁",
        executableWithoutArgs = true,
        nsfw = true,
        requiresFullMemberCache = true,
        aliases = {"bathfuck"}
)
public class BathroomFuckCommand extends RolePlayAbstract {

    public BathroomFuckCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220283477610729625/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220283805383004160/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220283866464518264/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220283937373425674/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220283998715121735/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220284060488831017/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220284131129294898/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220284200327057509/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220284257575108658/bathroomfuck.gif",
                "https://cdn.discordapp.com/attachments/1220283255580921856/1220284437393051678/bathroomfuck.gif"
        );
    }

}
