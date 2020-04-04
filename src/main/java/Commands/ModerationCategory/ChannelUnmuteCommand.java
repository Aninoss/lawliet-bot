package Commands.ModerationCategory;

import CommandListeners.CommandProperties;

import Constants.Permission;

@CommandProperties(
        trigger = "chunmute",
        userPermissions = Permission.MANAGE_CHANNEL_PERMISSIONS,
        botPermissions = Permission.MANAGE_CHANNEL_PERMISSIONS,
        thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/stop-icon.png",
        emoji = "\uD83D\uDED1",
        executable = false,
        aliases = {"channelunmute", "unmute", "unchmute", "unchannelmute"}
)
public class ChannelUnmuteCommand extends ChannelMuteCommand  {

    public ChannelUnmuteCommand() {
        super(false);
    }

}