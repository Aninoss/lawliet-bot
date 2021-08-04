package commands.runnables.interactionscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.RolePlayAbstract;

@CommandProperties(
        trigger = "throw",
        emoji = "\uD83D\uDCA8",
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class ThrowCommand extends RolePlayAbstract {

    public ThrowCommand(Locale locale, String prefix) {
        super(locale, prefix, true,
                "https://cdn.discordapp.com/attachments/736272093900963923/736272105422716990/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/736272109927530566/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/736272115216679033/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/736272120849629285/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/736272129246625948/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/736272137224060959/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/736272144731734056/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/736272152646647918/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/736272157025501295/throw.gif",
                "https://cdn.discordapp.com/attachments/736272093900963923/860571381597863946/throw.gif"
        );
    }

}
