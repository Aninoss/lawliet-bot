package commands.runnables.utilitycategory;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnStaticReactionRemoveListener;
import constants.Emojis;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicUser;
import core.cache.VoteCache;
import core.utils.*;
import modules.VoteInfo;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "vote",
        emoji = "\uD83D\uDDF3",
        executableWithoutArgs = false,
        aliases = {"poll"}
)
public class VoteCommand extends Command implements OnStaticReactionAddListener, OnStaticReactionRemoveListener {

    private static final UnicodeEmoji EMOJI_CANCEL = Emojis.X;
    private static final QuickUpdater quickUpdater = new QuickUpdater();

    private final boolean multiVote;

    public VoteCommand(Locale locale, String prefix) {
        this(locale, prefix, false);
    }

    public VoteCommand(Locale locale, String prefix, boolean multiVote) {
        super(locale, prefix);
        this.multiVote = multiVote;
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        if (args.startsWith("|")) {
            args = args.substring(1).trim();
        }

        GuildMessageChannel channel;
        CommandUtil.ChannelResponse response = CommandUtil.differentChannelExtract(this, event, event.getMessageChannel(), args, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY);
        if (response != null) {
            args = response.getArgs();
            channel = response.getChannel();
        } else {
            return false;
        }

        String[] argsParts = args.split("(?<!\\\\)\\|");
        for (int i = 0; i < argsParts.length; i++) {
            argsParts[i] = argsParts[i].replace("\\|", "|");
        }

        if (argsParts.length < 3 || argsParts.length > 13) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.UTILITY, "vote_wrong_args")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        String topic = argsParts[0].trim();
        if (topic.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.UTILITY, "vote_no_topic")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        String[] answers = new String[argsParts.length - 1];
        System.arraycopy(argsParts, 1, answers, 0, answers.length);
        for (int i = 0; i < answers.length; i++) {
            answers[i] = answers[i].replace("\n", "");
        }

        ArrayList<HashSet<Long>> userVotes = new ArrayList<>();
        for (int i = 0; i < answers.length; i++) {
            userVotes.add(new HashSet<>());
        }

        VoteInfo voteInfo = new VoteInfo(topic, answers, userVotes, event.getMember().getIdLong(), true, multiVote);
        EmbedBuilder eb = getEmbed(voteInfo, true);
        Message message = CommandUtil.differentChannelSendMessage(this, event, channel, eb, Collections.emptyMap()).get();
        registerStaticReactionMessage(message, event.getMember().getId());
        VoteCache.put(message.getIdLong(), voteInfo);

        RestActionQueue restActionQueue = new RestActionQueue();
        for (int i = 0; i < answers.length; i++) {
            restActionQueue.attach(message.addReaction(Emojis.LETTERS[i]));
        }
        restActionQueue.attach(message.addReaction(EMOJI_CANCEL))
                .getCurrentRestAction()
                .queue();
        return true;
    }

    public EmbedBuilder getEmbed(VoteInfo voteInfo, boolean open) {
        StringBuilder answerText = new StringBuilder();
        StringBuilder resultsText = new StringBuilder();

        for (int i = 0; i < voteInfo.getSize(); i++) {
            answerText.append(Emojis.LETTERS[i].getFormatted()).append(" | ").append(voteInfo.getChoices(i)).append("\n");
            resultsText.append(Emojis.LETTERS[i].getFormatted()).append(" | ").append(StringUtil.getBar((double) voteInfo.getUserVotes(i) / voteInfo.getTotalVotes(), 12)).append(" 【 ").append(voteInfo.getUserVotes(i)).append(" • ").append((int) (voteInfo.getPercentage(i) * 100)).append("% 】").append("\n");
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, "", getString("title"))
                .addField(TextManager.getString(getLocale(), Category.UTILITY, "vote_topic"), voteInfo.getTopic(), false)
                .addField(TextManager.getString(getLocale(), Category.UTILITY, "vote_choices"), StringUtil.shortenString(answerText.toString(), 1024), false)
                .addField(TextManager.getString(getLocale(), Category.UTILITY, "vote_results") + " (" + voteInfo.getTotalVotes() + " " + TextManager.getString(getLocale(), Category.UTILITY, "vote_votes", voteInfo.getTotalVotes() != 1) + ")", resultsText.toString(), false);

        if (voteInfo.getCreatorId().isPresent() && voteInfo.isActive()) {
            String authorString = voteInfo.isNewVersion()
                    ? AtomicUser.fromOutsideCache(voteInfo.getCreatorId().orElse(null)).getName(getLocale())
                    : String.valueOf(voteInfo.getCreatorId().get());
            eb.setFooter(TextManager.getString(getLocale(), Category.UTILITY, "vote_footer", authorString));
        }
        if (!open) {
            EmbedUtil.addLog(eb, LogStatus.WARNING, TextManager.getString(getLocale(), Category.UTILITY, "vote_closed"));
        }
        return eb;
    }

    @Override
    public void onStaticReactionAdd(@NotNull Message message, @NotNull MessageReactionAddEvent event) {
        VoteCache.get(event.getGuildChannel(), event.getMessageIdLong(), event.getUserIdLong(), event.getEmoji(), true, multiVote).ifPresent(voteInfo -> {
            if (EmojiUtil.equals(event.getEmoji(), EMOJI_CANCEL) &&
                    voteInfo.getCreatorId().isPresent() &&
                    voteInfo.getCreatorId().get() == event.getUserIdLong()
            ) {
                voteInfo.stop();
                DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).remove(event.getMessageIdLong());
                if (BotPermissionUtil.canWriteEmbed(event.getGuildChannel())) {
                    quickUpdater.update(
                            event.getMessageIdLong(),
                            event.getGuildChannel().editMessageEmbedsById(event.getMessageIdLong(), getEmbed(voteInfo, false).build())
                    );
                }
                if (BotPermissionUtil.can(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                    event.getGuildChannel().clearReactionsById(event.getMessageIdLong()).queue();
                }
                return;
            }

            if (voteInfo.getVotes(event.getUserIdLong()) > 1 && !multiVote && BotPermissionUtil.can(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                event.getGuildChannel()
                        .removeReactionById(event.getMessageIdLong(), event.getEmoji(), event.getUser())
                        .queue();
                return;
            }

            if (BotPermissionUtil.canWriteEmbed(event.getGuildChannel())) {
                quickUpdater.update(
                        event.getMessageIdLong(),
                        event.getGuildChannel().editMessageEmbedsById(event.getMessageIdLong(), getEmbed(voteInfo, true).build())
                );
            }
        });
    }

    @Override
    public void onStaticReactionRemove(@NotNull Message message, @NotNull MessageReactionRemoveEvent event) {
        VoteCache.get(event.getGuildChannel(), event.getMessageIdLong(), event.getUserIdLong(), event.getEmoji(), false, multiVote)
                .ifPresent(voteInfo -> {
                    if ((voteInfo.getVotes(event.getUserIdLong()) == 0 || multiVote) && BotPermissionUtil.canWriteEmbed(event.getGuildChannel())) {
                        quickUpdater.update(
                                event.getMessageIdLong(),
                                event.getGuildChannel().editMessageEmbedsById(event.getMessageIdLong(), getEmbed(voteInfo, true).build())
                        );
                    }
                });
    }

}
