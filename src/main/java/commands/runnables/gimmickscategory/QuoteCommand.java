package commands.runnables.gimmickscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.cache.MessageCache;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.MessageQuote;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        List<Message> directMessage = MentionUtil.getMessageWithLinks(event.getGuild(), args).get().getList();

        // message link
        if (!directMessage.isEmpty()) {
            for (Message message : directMessage) {
                if (BotPermissionUtil.canReadHistory(message.getGuildChannel())) {
                    try (MessageCreateData m = MessageQuote.postQuote(getPrefix(), getLocale(), event.getMessageChannel(), message, false)) {
                        setActionRows(m.getComponents().stream().map(c -> (ActionRow) c).collect(Collectors.toList()));
                        drawMessageNew(new EmbedBuilder(m.getEmbeds().get(0))).exceptionally(ExceptionLogger.get());
                        return true;
                    }
                }
            }
        }

        EmbedBuilder eb;
        if (!args.isEmpty()) {
            MentionList<GuildMessageChannel> channelMention = MentionUtil.getGuildMessageChannels(event.getGuild(), args);
            String newString = channelMention.getFilteredArgs();
            GuildMessageChannel channel = channelMention.getList().isEmpty() ? event.getMessageChannel() : channelMention.getList().get(0);

            // id with channel
            if (StringUtil.stringIsLong(newString)) {
                try {
                    Message message = MessageCache.retrieveMessage(channel, Long.parseLong(newString)).get();
                    if (JDAUtil.messageIsUserGenerated(message)) {
                        try (MessageCreateData m = MessageQuote.postQuote(getPrefix(), getLocale(), event.getMessageChannel(), message, false)) {
                            setActionRows(m.getComponents().stream().map(c -> (ActionRow) c).collect(Collectors.toList()));
                            drawMessageNew(new EmbedBuilder(m.getEmbeds().get(0))).exceptionally(ExceptionLogger.get());
                            return true;
                        }
                    }
                } catch (ExecutionException | InterruptedException e) {
                    //Ignore
                }
            }

            eb = EmbedFactory.getEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(getString("noresult_channel", newString, channel.getAsMention()));
        } else {
            eb = EmbedFactory.getEmbedError(this, getString("noarg", event.getMember().getAsMention()));
        }

        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return false;
    }

}
