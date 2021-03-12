package commands.runnables.gimmickscategory;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.cache.MessageCache;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.MessageQuote;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "quote",
        botGuildPermissions = Permission.MESSAGE_HISTORY,
        userChannelPermissions = Permission.MESSAGE_HISTORY,
        emoji = "\uD83D\uDCDD",
        executableWithoutArgs = false,
        aliases = { "qoute" }
)
public class QuoteCommand extends Command {

    public QuoteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        List<Message> directMessage = MentionUtil.getMessageWithLinks(event.getMessage(), args).get().getList();

        // message link
        if (directMessage.size() > 0) {
            for (Message message : directMessage) {
                if (BotPermissionUtil.canRead(message.getTextChannel())) {
                    MessageQuote.postQuote(getLocale(), event.getChannel(), message, false);
                    return true;
                }
            }
        }

        EmbedBuilder eb;
        if (args.length() > 0) {
            MentionList<TextChannel> channelMention = MentionUtil.getTextChannels(event.getMessage(), args);
            String newString = channelMention.getFilteredArgs();
            TextChannel channel = channelMention.getList().isEmpty() ? event.getChannel() : channelMention.getList().get(0);

            // id with channel
            if (StringUtil.stringIsLong(newString)) {
                try {
                    Message message = MessageCache.getInstance().retrieveMessage(channel, Long.parseLong(newString)).get();
                    MessageQuote.postQuote(getLocale(), event.getChannel(), message, false);
                    return true;
                } catch (ExecutionException | InterruptedException e) {
                    //Ignore
                }
            }

            eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(getString("noresult_channel", newString, channel.getAsMention()));
        } else {
            eb = EmbedFactory.getEmbedError(this, getString("noarg", event.getMessage().getMember().getAsMention()));
        }

        event.getChannel().sendMessage(eb.build()).queue();
        return false;
    }

}
