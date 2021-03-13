package commands.runnables.moderationcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import net.dv8tion.jda.api.Permission;

@CommandProperties(
        trigger = "chunmute",
        userGuildPermissions = { Permission.MANAGE_ROLES },
        emoji = "\uD83D\uDED1",
        executableWithoutArgs = false,
        aliases = { "channelunmute", "unmute", "unchmute", "unchannelmute" }
)
public class ChannelUnmuteCommand extends ChannelMuteCommand {

    public ChannelUnmuteCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

}