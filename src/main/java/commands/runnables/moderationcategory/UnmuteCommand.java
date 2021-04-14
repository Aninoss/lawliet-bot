package commands.runnables.moderationcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import net.dv8tion.jda.api.Permission;

@CommandProperties(
        trigger = "unmute",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "🛑",
        executableWithoutArgs = false,
        releaseDate = { 2021, 4, 16 },
        aliases = { "chunmute", "channelunmute", "demute" }
)
public class UnmuteCommand extends MuteCommand {

    public UnmuteCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

}
