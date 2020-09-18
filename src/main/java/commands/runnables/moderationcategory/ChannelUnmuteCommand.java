package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import constants.Permission;

import java.util.Locale;

@CommandProperties(
        trigger = "chunmute",
        userPermissions = Permission.MANAGE_CHANNEL_PERMISSIONS | Permission.MANAGE_CHANNEL,
        botPermissions = Permission.MANAGE_CHANNEL_PERMISSIONS | Permission.MANAGE_CHANNEL,
        emoji = "\uD83D\uDED1",
        executable = false,
        aliases = {"channelunmute", "unmute", "unchmute", "unchannelmute"}
)
public class ChannelUnmuteCommand extends ChannelMuteCommand  {

    public ChannelUnmuteCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

}