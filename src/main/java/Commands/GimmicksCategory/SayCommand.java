package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Permission;
import Core.EmbedFactory;
import Core.Mention.MentionUtil;
import Core.Mention.MentionList;
import Core.PermissionCheck;
import Core.TextManager;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
        trigger = "say",
        emoji = "\uD83D\uDCAC",
        executable = false,
        aliases = {"repeat"}
)
public class SayCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        event.getChannel().sendMessage(EmbedFactory.getEmbed().setDescription(followedString).setFooter(event.getMessage().getUserAuthor().get().getDiscriminatedName())).get();
        return true;
    }

}
