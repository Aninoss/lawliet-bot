package commands.runnables.interactionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "no",
        emoji = "\uD83D\uDC4E",
        executableWithoutArgs = true,
        aliases = { "nope", "thumbsdown", "thumpsdown" }
)
public class NoCommand extends RolePlayAbstract {

    public NoCommand(Locale locale, String prefix) {
        super(locale, prefix, false);

        setFtaGifs(
                "https://cdn.discordapp.com/attachments/736254603410014320/736254607449128981/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254609856659577/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254620514648114/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254624616415252/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254630455017502/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254652554936380/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254656174620742/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254666060595271/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254685232496703/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254700134858883/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254707630211172/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254717839278100/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254733131579482/no.gif"
        );
        setMtaGifs(
                "https://cdn.discordapp.com/attachments/736254603410014320/736254616714608651/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254633781231767/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254674956714104/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254725196087427/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254738223333517/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/743053003820498964/no.gif"
        );
        setAtaGifs(
                "https://cdn.discordapp.com/attachments/736254603410014320/736254644367654992/no.gif",
                "https://cdn.discordapp.com/attachments/736254603410014320/736254688915226709/no.gif"
        );
    }

}
