package commands.runnables.interactionscategory;
import commands.listeners.CommandProperties;
import commands.runnables.InteractionAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "throw",
        emoji = "\uD83D\uDCA8",
        executableWithoutArgs = true
)
public class ThrowCommand extends InteractionAbstract {

    public ThrowCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected String[] getGifs() {
        return new String[]{
                "https://media.discordapp.net/attachments/736272093900963923/736272105422716990/throw.gif",
                "https://media.discordapp.net/attachments/736272093900963923/736272109927530566/throw.gif",
                "https://media.discordapp.net/attachments/736272093900963923/736272115216679033/throw.gif",
                "https://media.discordapp.net/attachments/736272093900963923/736272120849629285/throw.gif",
                "https://media.discordapp.net/attachments/736272093900963923/736272129246625948/throw.gif",
                "https://media.discordapp.net/attachments/736272093900963923/736272137224060959/throw.gif",
                "https://media.discordapp.net/attachments/736272093900963923/736272144731734056/throw.gif",
                "https://media.discordapp.net/attachments/736272093900963923/736272152646647918/throw.gif",
                "https://media.discordapp.net/attachments/736272093900963923/736272157025501295/throw.gif"
        };
    }

}
