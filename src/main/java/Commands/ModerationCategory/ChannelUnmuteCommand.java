package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import Constants.Permission;

@CommandProperties(
        trigger = "chunmute",
        userPermissions = Permission.MANAGE_PERMISSIONS_IN_CHANNEL,
        botPermissions = Permission.MANAGE_PERMISSIONS_IN_CHANNEL,
        thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/stop-icon.png",
        emoji = "\uD83D\uDED1",
        executable = false,
        aliases = {"channelunmute", "unmute", "unchmute", "unchannelmute"}
)
public class ChannelUnmuteCommand extends ChannelMuteCommand implements onRecievedListener  {

    public ChannelUnmuteCommand() {
        super(false);
    }

}