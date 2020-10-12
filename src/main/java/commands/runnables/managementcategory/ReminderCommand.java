package commands.runnables.managementcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import constants.Permission;
import core.EmbedFactory;
import core.mention.MentionList;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "reminder",
        userPermissions = Permission.MANAGE_SERVER | Permission.MENTION_EVERYONE,
        emoji = "⏲️",
        executableWithoutArgs = false,
        aliases = { "remindme", "remind" }
)
public class ReminderCommand extends Command implements OnReactionAddListener {

    private final String CANCEL_EMOJI = "❌";

    private Message message = null;

    public ReminderCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        long minutes = 0;
        StringBuilder text = new StringBuilder();
        MentionList<ServerTextChannel> channelMention = MentionUtil.getTextChannels(event.getMessage(), followedString);
        followedString = channelMention.getResultMessageString();

        ArrayList<ServerTextChannel> channels = channelMention.getList();
        if (channels.size() > 1) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("twochannels"))).get();
            return false;
        }
        ServerTextChannel channel = channels.size() == 0 ? event.getServerTextChannel().get() : channels.get(0);

        for(String part : followedString.split(" ")) {
            if (part.length() > 0) {
                long value = MentionUtil.getTimeMinutesExt(part);
                if (value > 0) {
                    minutes += value;
                } else {
                    text.append(part).append(" ");
                }
            } else {
                text.append(" ");
            }
        }

        if (minutes <= 0 || minutes > 366 * 24 * 60) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("notime"))).get();
            return false;
        }

        String messageText = StringUtil.trimString(text.toString());
        if (messageText.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("notext"))).get();
            return false;
        }

        String timeSpanString = TimeUtil.getRemainingTimeString(getLocale(), minutes * 60 * 1000, false);
        message = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("template", channel.getMentionTag(), timeSpanString, messageText, CANCEL_EMOJI))).get();
        message.addReaction(CANCEL_EMOJI).get();

        return true;
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals(CANCEL_EMOJI)) {
            removeReactionListener();
            message.edit(EmbedFactory.getCommandEmbedStandard(this, getString("canceled"))).get();
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}

}
