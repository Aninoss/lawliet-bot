package commands.runnables.utilitycategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnStaticReactionRemoveListener;
import constants.Emojis;
import constants.LogStatus;
import core.*;
import core.cache.VoteCache;
import core.utils.*;
import modules.VoteInfo;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "vote",
        emoji = "\uD83D\uDDF3",
        executableWithoutArgs = false,
        aliases = { "poll" }
)
public class VoteCommand extends Command implements OnStaticReactionAddListener, OnStaticReactionRemoveListener {

    private static final QuickUpdater quickUpdater = new QuickUpdater();

    private final String EMOJI_CANCEL = Emojis.X;

    public VoteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        args = args.replace("\n", "");
        if (args.startsWith("|")) {
            args = args.substring(1).trim();
        }

        BaseGuildMessageChannel channel;
        CommandUtil.ChannelResponse response = CommandUtil.differentChannelExtract(this, event, args, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY);
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

        if (argsParts.length >= 3 && argsParts.length <= 13) {
            String topic = argsParts[0].trim();
            if (topic.isEmpty()) {
                drawMessageNew(EmbedFactory.getEmbedError(this, getString("no_topic")))
                        .exceptionally(ExceptionLogger.get());
                return false;
            } else {
                String[] answers = new String[argsParts.length - 1];
                System.arraycopy(argsParts, 1, answers, 0, answers.length);
                ArrayList<HashSet<Long>> userVotes = new ArrayList<>();
                for (int i = 0; i < answers.length; i++) {
                    userVotes.add(new HashSet<>());
                }

                VoteInfo voteInfo = new VoteInfo(topic, answers, userVotes, event.getMember().getIdLong());
                EmbedBuilder eb = getEmbed(voteInfo, true);
                Message message = CommandUtil.differentChannelSendMessage(this, event, channel, eb, Collections.emptyMap());
                registerStaticReactionMessage(message);
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
        } else {
            drawMessageNew(EmbedFactory.getEmbedError(this, getString("wrong_args")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    public EmbedBuilder getEmbed(VoteInfo voteInfo, boolean open) {
        StringBuilder answerText = new StringBuilder();
        StringBuilder resultsText = new StringBuilder();

        for (int i = 0; i < voteInfo.getSize(); i++) {
            answerText.append(Emojis.LETTERS[i]).append(" | ").append(voteInfo.getChoices(i)).append("\n");
            resultsText.append(Emojis.LETTERS[i]).append(" | ").append(StringUtil.getBar((double) voteInfo.getUserVotes(i) / voteInfo.getTotalVotes(), 12)).append(" 【 ").append(voteInfo.getUserVotes(i)).append(" • ").append((int) (voteInfo.getPercentage(i) * 100)).append("% 】").append("\n");
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, "", getString("title"))
                .addField(getString("topic"), voteInfo.getTopic(), false)
                .addField(getString("choices"), StringUtil.shortenString(answerText.toString(), 1024), false)
                .addField(getString("results") + " (" + voteInfo.getTotalVotes() + " " + getString("votes", voteInfo.getTotalVotes() != 1) + ")", resultsText.toString(), false);

        if (voteInfo.getCreatorId().isPresent() && voteInfo.isActive()) {
            eb.setFooter(getString("footer", String.valueOf(voteInfo.getCreatorId().get())));
        }

        if (!open) EmbedUtil.addLog(eb, LogStatus.WARNING, getString("closed"));

        return eb;
    }

    @Override
    public void onStaticReactionAdd(@NotNull Message message, @NotNull MessageReactionAddEvent event) {
        VoteCache.get(event.getGuildChannel(), event.getMessageIdLong(), event.getUserIdLong(), EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()), true).ifPresent(voteInfo -> {
            if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), EMOJI_CANCEL) &&
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

            if (voteInfo.getVotes(event.getUserIdLong()) > 1 && BotPermissionUtil.can(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                event.getGuildChannel()
                        .removeReactionById(event.getMessageIdLong(), EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()), event.getUser())
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
        VoteCache.get(event.getGuildChannel(), event.getMessageIdLong(), event.getUserIdLong(), EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()), false)
                .ifPresent(voteInfo -> {
                    if (voteInfo.getVotes(event.getUserIdLong()) == 0) {
                        if (BotPermissionUtil.canWriteEmbed(event.getGuildChannel())) {
                            quickUpdater.update(
                                    event.getMessageIdLong(),
                                    event.getGuildChannel().editMessageEmbedsById(event.getMessageIdLong(), getEmbed(voteInfo, true).build())
                            );
                        }
                    }
                });
    }

}
