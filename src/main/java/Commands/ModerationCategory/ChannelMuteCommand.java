package Commands.ModerationCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Permission;
import Core.*;
import Core.Mention.Mention;
import Core.Mention.MentionUtil;
import Modules.Mute.MuteData;
import Modules.Mute.MuteManager;
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
        trigger = "chmute",
        userPermissions = Permission.MANAGE_CHANNEL_PERMISSIONS | Permission.MANAGE_CHANNEL,
        botPermissions = Permission.MANAGE_CHANNEL_PERMISSIONS | Permission.MANAGE_CHANNEL,
        emoji = "\uD83D\uDED1",
        executable = false,
        aliases = {"channelmute", "mute"}
)
public class ChannelMuteCommand extends Command  {

    private final boolean mute;

    public ChannelMuteCommand() {
        this.mute = true;
    }

    public ChannelMuteCommand(boolean mute) {
        this.mute = mute;
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        Server server = message.getServer().get();

        ServerTextChannel channel = message.getServerTextChannel().get();
        List<ServerTextChannel> channelList = MentionUtil.getTextChannels(message, followedString).getList();
        if (channelList.size() > 0)
            channel = channelList.get(0);

        EmbedBuilder errorEmbed = PermissionCheck.getUserAndBotPermissionMissingEmbed(getLocale(), server, channel, message.getUserAuthor().get(), getUserPermissions(), getBotPermissions());
        if (errorEmbed != null) {
            message.getChannel().sendMessage(errorEmbed).get();
            return false;
        }

        List<User> userList = MentionUtil.getUsers(message, followedString).getList();
        if (userList.size() == 0) {
            message.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        ArrayList<User> successfulUsers = new ArrayList<>();
        for(User user: userList) {
            if (!PermissionCheck.hasAdminPermissions(server, user)) successfulUsers.add(user);
        }

        if (successfulUsers.size() == 0) {
            message.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"admin_block"))).get();
            return false;
        }

        MuteData muteData = new MuteData(server, channel, successfulUsers);
        boolean doneSomething = MuteManager.getInstance().executeMute(muteData, mute);

        Mention mention = MentionUtil.getMentionedStringOfUsers(getLocale(), channel.getServer(), userList);
        EmbedBuilder actionEmbed = EmbedFactory.getCommandEmbedStandard(this, getString("action", mention.isMultiple(), mention.toString(), message.getUserAuthor().get().getMentionTag(), channel.getMentionTag()));
        for(User user: userList) {
            try {
                if (!user.isYourself() && !user.isBot()) user.sendMessage(actionEmbed).get();
            } catch (ExecutionException e) {
                //Ignore
            }
        }

        if (doneSomething)
            ModSettingsCommand.postLog(this, actionEmbed, event.getServer().get());

        if (!mute || !successfulUsers.contains(DiscordApiCollection.getInstance().getYourself()) || channel.getId() != event.getServerTextChannel().get().getId()) {
            EmbedBuilder eb;

            if (doneSomething)
                eb = EmbedFactory.getCommandEmbedStandard(this, getString("success_description", mention.isMultiple(), mention.toString(), channel.getMentionTag()));
            else
                eb = EmbedFactory.getCommandEmbedError(this, getString("nothingdone", mention.isMultiple(), mention.toString(), channel.getMentionTag()));

            event.getChannel().sendMessage(eb).get();
        }

        return true;
    }

}