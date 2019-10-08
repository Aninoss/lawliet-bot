package Commands.Moderation;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.*;
import General.Mention.Mention;
import General.Mention.MentionFinder;
import General.Mute.MuteData;
import General.Mute.MuteManager;
import MySQL.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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