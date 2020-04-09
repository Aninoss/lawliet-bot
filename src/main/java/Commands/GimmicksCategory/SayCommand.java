package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Permission;
import Core.EmbedFactory;
import Core.Mention.MentionTools;
import Core.Mention.MentionList;
import Core.PermissionCheck;
import Core.TextManager;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "say",
        emoji = "\uD83D\uDCAC",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/arrow-refresh-4-icon.png",
        executable = false,
        aliases = {"repeat"}
)
public class SayCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        MentionList<ServerTextChannel> mentionedChannels = MentionTools.getTextChannels(event.getMessage(), followedString, true);
        followedString = mentionedChannels.getResultMessageString();

        ServerTextChannel postChannel = event.getServerTextChannel().get();
        if (mentionedChannels.getList().size() > 0) postChannel = mentionedChannels.getList().get(0);

        int permissions = Permission.SEND_MESSAGES | Permission.EMBED_LINKS;
        EmbedBuilder errorEmbed = PermissionCheck.getUserAndBotPermissionMissingEmbed(getLocale(), event.getServer().get(), postChannel, event.getMessage().getUserAuthor().get(), permissions, permissions);
        if (errorEmbed != null) {
            event.getChannel().sendMessage(errorEmbed).get();
            return false;
        }

        if (followedString.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL,"no_args"))).get();
            return false;
        }

        postChannel.sendMessage(EmbedFactory.getEmbed().setDescription(followedString).setFooter(event.getMessage().getUserAuthor().get().getDiscriminatedName())).get();
        return true;
    }

}
