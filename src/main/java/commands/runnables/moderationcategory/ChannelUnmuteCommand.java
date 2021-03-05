package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import constants.PermissionDeprecated;

import java.util.Locale;

@CommandProperties(
        trigger = "chunmute",
        userPermissions = PermissionDeprecated.MANAGE_CHANNEL_PERMISSIONS | PermissionDeprecated.MANAGE_CHANNEL,
        botPermissions = PermissionDeprecated.MANAGE_CHANNEL_PERMISSIONS | PermissionDeprecated.MANAGE_CHANNEL,
        emoji = "\uD83D\uDED1",
        executableWithoutArgs = false,
        aliases = {"channelunmute", "unmute", "unchmute", "unchannelmute"}
)
public class ChannelUnmuteCommand extends ChannelMuteCommand  {

    public ChannelUnmuteCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

}