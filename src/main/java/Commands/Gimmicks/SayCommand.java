package Commands.Gimmicks;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.EmbedFactory;
import General.Mention.MentionFinder;
import General.Mention.MentionList;
import General.PermissionCheck;
import General.TextManager;
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
public class SayCommand extends Command implements onRecievedListener {

    public SayCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        MentionList<ServerTextChannel> mentionedChannels = MentionFinder.getTextChannels(event.getMessage(), followedString);
        followedString = mentionedChannels.getResultMessageString();

        ServerTextChannel postChannel = event.getServerTextChannel().get();
        if (mentionedChannels.getList().size() > 0) postChannel = mentionedChannels.getList().get(0);

        int permissions = Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS;
        EmbedBuilder errorEmbed = PermissionCheck.userAndBothavePermissions(getLocale(), event.getServer().get(), postChannel, event.getMessage().getUserAuthor().get(), permissions, permissions);
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
