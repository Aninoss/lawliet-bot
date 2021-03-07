package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;
import commands.Command;
import core.EmbedFactory;
import core.ShardManager;
import core.mention.MentionList;
import core.utils.MentionUtil;
import core.TextManager;
import core.utils.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "quote",
        botPermissions = PermissionDeprecated.READ_MESSAGE_HISTORY,
        userPermissions = PermissionDeprecated.READ_MESSAGE_HISTORY,
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = false,
        aliases = {"qoute"}
)
public class QuoteCommand extends Command {

    public QuoteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        User user = event.getMessage().getUserAuthor().get();

        //Message Link
        ArrayList<Message> directMessage = MentionUtil.getMessageWithLinks(event.getMessage(), followedString).getList();
        if (directMessage.size() > 0) {
            for(Message message : directMessage) {
                if (message.getChannel().canSee(user) && message.getChannel().canReadMessageHistory(user)) {
                    postEmbed(event.getMessage().getServerTextChannel().get(), message);
                    return true;
                }
            }
        }

        if (followedString.length() > 0) {
            MentionList<ServerTextChannel> channelMention = MentionUtil.getTextChannels(event.getMessage(), followedString);
            String newString = channelMention.getResultMessageString();
            ServerTextChannel channel = channelMention.getList().isEmpty() ? event.getServerTextChannel().get() : channelMention.getList().get(0);

            //ID with channel
            if (StringUtil.stringIsLong(newString)) {
                Message message = ShardManager.getInstance().getMessageById(channel, Long.parseLong(newString)).join().orElse(null);
                if (message != null) {
                    postEmbed(event.getMessage().getServerTextChannel().get(), message);
                    return true;
                }
            }

            EmbedBuilder eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(),TextManager.GENERAL,"no_results"))
                    .setDescription(getString("noresult_channel", newString, channel.getMentionTag()));
            event.getChannel().sendMessage(eb).get();
            return false;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("noarg", event.getMessage().getUserAuthor().get().getMentionTag()));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
    }

    public void postEmbed(ServerTextChannel channel, Message searchedMessage) throws IOException, ExecutionException, InterruptedException {
        postEmbed(channel, searchedMessage, false);
    }
}
