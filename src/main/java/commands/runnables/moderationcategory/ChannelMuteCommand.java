package commands.runnables.moderationcategory;

import java.util.ArrayList;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ShardManager;
import core.TextManager;
import core.mention.Mention;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import modules.Mod;
import modules.mute.MuteData;
import modules.mute.MuteManager;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "chmute",
        userPermissions = PermissionDeprecated.MANAGE_CHANNEL_PERMISSIONS | PermissionDeprecated.MANAGE_CHANNEL,
        botPermissions = PermissionDeprecated.MANAGE_CHANNEL_PERMISSIONS | PermissionDeprecated.MANAGE_CHANNEL,
        emoji = "\uD83D\uDED1",
        executableWithoutArgs = false,
        aliases = {"channelmute", "mute"}
)
public class ChannelMuteCommand extends Command {

    private final boolean mute;

    public ChannelMuteCommand(Locale locale, String prefix) {
        super(locale, prefix);
        this.mute = true;
    }

    public ChannelMuteCommand(Locale locale, String prefix, boolean mute) {
        super(locale, prefix);
        this.mute = mute;
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Message message = event.getMessage();
        Server server = message.getServer().get();

        ServerTextChannel channel = message.getServerTextChannel().get();
        List<ServerTextChannel> channelList = MentionUtil.getTextChannels(message, followedString).getList();
        if (channelList.size() > 0)
            channel = channelList.get(0);

        EmbedBuilder errorEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(getLocale(), server, channel, message.getUserAuthor().get(), getUserPermissions(), getBotPermissions());
        if (errorEmbed != null) {
            message.getChannel().sendMessage(errorEmbed).get();
            return false;
        }

        List<User> userList = MentionUtil.getMembers(message, followedString).getList();
        if (userList.size() == 0) {
            message.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        ArrayList<User> successfulUsers = new ArrayList<>();
        for(User user: userList) {
            if (!BotPermissionUtil.hasAdminPermissions(server, user)) successfulUsers.add(user);
        }

        if (successfulUsers.size() == 0) {
            message.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"admin_block"))).get();
            return false;
        }

        MuteData muteData = new MuteData(server, channel, successfulUsers);
        boolean doneSomething = MuteManager.getInstance().executeMute(muteData, mute);

        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), userList);
        EmbedBuilder actionEmbed = EmbedFactory.getEmbedDefault(this, getString("action", mention.isMultiple(), mention.getMentionText(), message.getUserAuthor().get().getMentionTag(), channel.getMentionTag()));

        if (doneSomething)
            Mod.postLog(this, actionEmbed, event.getServer().get(), userList).join();

        if (!mute || !successfulUsers.contains(ShardManager.getInstance().getSelf()) || channel.getId() != event.getServerTextChannel().get().getId()) {
            EmbedBuilder eb;

            if (doneSomething)
                eb = EmbedFactory.getEmbedDefault(this, getString("success_description", mention.isMultiple(), mention.getMentionText(), channel.getMentionTag()));
            else
                eb = EmbedFactory.getEmbedError(this, getString("nothingdone", mention.isMultiple(), mention.getMentionText(), channel.getMentionTag()));

            event.getChannel().sendMessage(eb).get();
        }

        return true;
    }

}