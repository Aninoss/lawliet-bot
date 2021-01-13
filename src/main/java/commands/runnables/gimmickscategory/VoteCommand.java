package commands.runnables.gimmickscategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddStaticListener;
import commands.listeners.OnReactionRemoveStaticListener;
import constants.Emojis;
import constants.LetterEmojis;
import constants.LogStatus;
import constants.Permission;
import core.EmbedFactory;
import core.QuickUpdater;
import core.cache.VoteCache;
import core.utils.DiscordUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.VoteInfo;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.ReactionRemoveEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;

@CommandProperties(
        trigger = "vote",
        botPermissions = Permission.MANAGE_MESSAGES | Permission.READ_MESSAGE_HISTORY,
        emoji = "\uD83D\uDDF3",
        executableWithoutArgs = false,
        aliases = { "poll" }
)
public class VoteCommand extends Command implements OnReactionAddStaticListener, OnReactionRemoveStaticListener {

    public VoteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        followedString = StringUtil.trimString(followedString.replace("\n", ""));
        if (followedString.startsWith("|")) followedString = followedString.substring(1);
        String[] args = followedString.split("\\|");
        if (args.length >= 3 && args.length <= 13) {
            String topic = StringUtil.trimString(args[0]);

            if (topic.length() == 0) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("no_topic")));
                return false;
            } else {
                String[] answers = new String[args.length - 1];
                System.arraycopy(args, 1, answers, 0, answers.length);
                ArrayList<HashSet<Long>> userVotes = new ArrayList<>();
                for (int i = 0; i < answers.length; i++) {
                    userVotes.add(new HashSet<>());
                }

                VoteInfo voteInfo = new VoteInfo(topic, answers, userVotes, event.getMessage().getUserAuthor().get().getId());
                EmbedBuilder eb = getEmbed(voteInfo, true);
                Message message = event.getServerTextChannel().get().sendMessage(eb).get();
                message.addReaction("❌").exceptionally(ExceptionLogger.get());
                for (int i = 0; i < answers.length; i++) {
                    message.addReaction(LetterEmojis.LETTERS[i]).exceptionally(ExceptionLogger.get());
                }
                return true;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("wrong_args")));
            return false;
        }
    }

    public EmbedBuilder getEmbed(VoteInfo voteInfo, boolean open) {
        StringBuilder answerText = new StringBuilder();
        StringBuilder resultsText = new StringBuilder();

        for(int i=0; i < voteInfo.getSize(); i++) {
            answerText.append(LetterEmojis.LETTERS[i]).append(" | ").append(voteInfo.getChoices(i)).append("\n");
            resultsText.append(LetterEmojis.LETTERS[i]).append(" | ").append(StringUtil.getBar((double) voteInfo.getUserVotes(i) / voteInfo.getTotalVotes(),12)).append(" 【 ").append(voteInfo.getUserVotes(i)).append(" • ").append((int)(voteInfo.getPercantage(i)*100)).append("% 】").append("\n");
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, "", getString("title") + (open ? Emojis.EMPTY_EMOJI : ""))
                .addField(getString("topic"), voteInfo.getTopic(),false)
                .addField(getString("choices"), answerText.toString(),false)
                .addField(getString("results") + " (" + voteInfo.getTotalVotes() + " " + getString("votes", voteInfo.getTotalVotes() != 1) + ")",resultsText.toString(),false);

        if (voteInfo.getCreatorId().isPresent())
            eb.setFooter(getString("footer", String.valueOf(voteInfo.getCreatorId().get())));

        if (!open) EmbedUtil.addLog(eb, LogStatus.WARNING, getString("closed"));

        return eb;
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        if (message.getEmbeds().size() == 0) return;

        VoteCache.getInstance().get(message, event.getUserId(), event.getEmoji().getMentionTag(), true).ifPresent(voteInfo -> {
            if (DiscordUtil.emojiIsString(event.getEmoji(), "❌") &&
                    voteInfo.getCreatorId().isPresent() &&
                    voteInfo.getCreatorId().get() == event.getUserId() &&
                    message.getReactions().size() > 0
            ) {
                QuickUpdater.getInstance().update(
                        getTrigger(),
                        message.getId(),
                        () -> message.edit(getEmbed(voteInfo, false))
                );
                voteInfo.stop();
                if (event.getServerTextChannel().get().canYouRemoveReactionsOfOthers())
                    message.removeAllReactions();
                return;
            }

            if (voteInfo.getVotes(event.getUserId()) > 1) {
                event.removeReaction().exceptionally(ExceptionLogger.get());
                return;
            }

            QuickUpdater.getInstance().update(
                    getTrigger(),
                    message.getId(),
                    () -> message.edit(getEmbed(voteInfo, true))
            );
        });
    }

    @Override
    public void onReactionRemoveStatic(Message message, ReactionRemoveEvent event) throws Throwable {
        if (message.getEmbeds().size() == 0) return;

        VoteCache.getInstance().get(message, event.getUserId(), event.getEmoji().getMentionTag(), false).ifPresent(voteInfo -> {
            if (voteInfo.getVotes(event.getUserId()) == 0) {
                QuickUpdater.getInstance().update(
                        getTrigger(),
                        message.getId(),
                        () -> message.edit(getEmbed(voteInfo, true))
                );
            }
        });
    }

    @Override
    public String getTitleStartIndicator() {
        return getEmoji();
    }
}
