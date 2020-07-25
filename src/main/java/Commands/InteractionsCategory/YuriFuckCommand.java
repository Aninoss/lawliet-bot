package Commands.InteractionsCategory;
import CommandListeners.CommandProperties;
import Commands.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "yurifuck",
        emoji = "\uD83D\uDC69\uD83D\uDECF\uD83D\uDC69️",
        executable = true,
        nsfw = true
)
public class YuriFuckCommand extends InteractionAbstract {

    public YuriFuckCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736283166758797324/736283244990955611/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283255959060520/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283260031860786/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283266994405406/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283284023017492/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283293082714193/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283297788854282/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283343087206550/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283350062334032/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283359470157844/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283393096024134/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283405695713380/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283415682351104/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283431201276014/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283456941719642/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283473274339388/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283490856992858/yurifuck.gif",
                "https://media.discordapp.net/attachments/736283166758797324/736283499455053854/yurifuck.gif"
        };
    }

}
