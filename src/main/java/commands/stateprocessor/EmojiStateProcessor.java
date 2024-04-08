package commands.stateprocessor;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.ExceptionLogger;
import core.ShardManager;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EmojiStateProcessor extends AbstractStateProcessor<Emoji, EmojiStateProcessor> {

    public EmojiStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_emoji_desc"));
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input) {
        NavigationAbstract command = getCommand();
        List<Emoji> emojis = MentionUtil.getEmojis(event.getMessage(), input).getList();

        if (emojis.isEmpty()) {
            command.setLog(LogStatus.FAILURE, TextManager.getNoResultsString(command.getLocale(), input));
            return MessageInputResponse.FAILED;
        }

        return processEmoji(emojis.get(0)) ? MessageInputResponse.SUCCESS : MessageInputResponse.FAILED;
    }

    public boolean handleReactionEvent(@NotNull GenericMessageReactionEvent event) {
        NavigationAbstract command = getCommand();
        if (command.getState() == getState() && event instanceof MessageReactionAddEvent) {
            processEmoji(event.getEmoji());
            command.processDraw(event.getMember(), true).exceptionally(ExceptionLogger.get());
            if (BotPermissionUtil.can(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                event.getReaction().removeReaction(event.getUser()).queue();
            }
        }

        return false;
    }

    private boolean processEmoji(Emoji emoji) {
        NavigationAbstract command = getCommand();
        if (emojiIsInaccessible(emoji)) {
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "emojiunknown", emoji.getName()));
            return false;
        }

        set(emoji);
        return true;
    }

    private boolean emojiIsInaccessible(Emoji emoji) {
        return emoji instanceof CustomEmoji && !ShardManager.customEmojiIsKnown((CustomEmoji) emoji);
    }

}
