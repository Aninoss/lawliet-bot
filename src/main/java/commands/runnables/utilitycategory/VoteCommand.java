package commands.runnables.utilitycategory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnStaticReactionRemoveListener;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.QuickUpdater;
import core.RestActionQueue;
import core.cache.VoteCache;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import modules.VoteInfo;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        args = args.replace("\n", "").trim();
        if (args.startsWith("|")) args = args.substring(1);
        String[] argsParts = args.split("\\|");
        if (argsParts.length >= 3 && argsParts.length <= 13) {
            String topic = argsParts[0].trim();

            if (topic.length() == 0) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("no_topic")).build())
                        .queue();
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
                Message message = event.getChannel().sendMessage(eb.build()).complete();
                DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).put(message.getIdLong(), new StaticReactionMessageData(message, getTrigger()));
                VoteCache.getInstance().put(message.getIdLong(), voteInfo);

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
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("wrong_args")).build())
                    .queue();
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
                .addField(getString("choices"), answerText.toString(), false)
                .addField(getString("results") + " (" + voteInfo.getTotalVotes() + " " + getString("votes", voteInfo.getTotalVotes() != 1) + ")", resultsText.toString(), false);

        if (voteInfo.getCreatorId().isPresent() && voteInfo.isActive()) {
            eb.setFooter(getString("footer", String.valueOf(voteInfo.getCreatorId().get())));
        }

        if (!open) EmbedUtil.addLog(eb, LogStatus.WARNING, getString("closed"));

        return eb;
    }

    @Override
    public void onStaticReactionAdd(Message message, GuildMessageReactionAddEvent event) {
        VoteCache.getInstance().get(event.getChannel(), event.getMessageIdLong(), event.getUserIdLong(), EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()), true).ifPresent(voteInfo -> {
            if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), EMOJI_CANCEL) &&
                    voteInfo.getCreatorId().isPresent() &&
                    voteInfo.getCreatorId().get() == event.getUserIdLong()
            ) {
                voteInfo.stop();
                DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).remove(event.getMessageIdLong());
                if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                    quickUpdater.update(
                            event.getMessageIdLong(),
                            event.getChannel().editMessageById(event.getMessageIdLong(), getEmbed(voteInfo, false).build())
                    );
                }
                if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                    event.getChannel().clearReactionsById(event.getMessageIdLong()).queue();
                }
                return;
            }

            if (voteInfo.getVotes(event.getUserIdLong()) > 1 && BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                event.getChannel()
                        .removeReactionById(event.getMessageIdLong(), EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()), event.getUser())
                        .queue();
                return;
            }

            if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                quickUpdater.update(
                        event.getMessageIdLong(),
                        event.getChannel().editMessageById(event.getMessageIdLong(), getEmbed(voteInfo, true).build())
                );
            }
        });
    }

    @Override
    public void onStaticReactionRemove(Message message, GuildMessageReactionRemoveEvent event) {
        VoteCache.getInstance().get(event.getChannel(), event.getMessageIdLong(), event.getUserIdLong(), EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()), false)
                .ifPresent(voteInfo -> {
                    if (voteInfo.getVotes(event.getUserIdLong()) == 0) {
                        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                            quickUpdater.update(
                                    event.getMessageIdLong(),
                                    event.getChannel().editMessageById(event.getMessageIdLong(), getEmbed(voteInfo, true).build())
                            );
                        }
                    }
                });
    }

    @Override
    public String titleStartIndicator() {
        return getCommandProperties().emoji();
    }

}
