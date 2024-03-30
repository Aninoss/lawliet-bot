package core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import constants.Emojis;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import modules.VoteInfo;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class VoteCache {

    private static final Cache<Long, VoteInfo> voteCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(60))
            .build();

    public static void put(long messageId, VoteInfo voteInfo) {
        voteCache.put(messageId, voteInfo);
    }

    public static Optional<VoteInfo> get(GuildMessageChannel channel, long messageId, long userId, Emoji emoji, boolean add, boolean multiVote) {
        VoteInfo voteInfo = voteCache.getIfPresent(messageId);
        if (voteInfo != null) {
            int i = -1;
            for (int j = 0; j < voteInfo.getSize(); j++) {
                if (EmojiUtil.equals(emoji, Emojis.LETTERS[j])) {
                    i = j;
                    break;
                }
            }

            if (!voteInfo.isActive() || i < 0 && !EmojiUtil.equals(emoji, Emojis.X)) {
                return Optional.empty();
            }

            if (i >= 0) {
                if (add) {
                    voteInfo.addVote(i, userId);
                } else {
                    voteInfo.removeVote(i, userId);
                }
            }
        } else {
            Message message = channel.retrieveMessageById(messageId).complete();
            voteInfo = extractVoteInfoFromMessage(message, multiVote);
            voteCache.put(message.getIdLong(), voteInfo);
        }

        return Optional.of(voteInfo);
    }

    private static VoteInfo extractVoteInfoFromMessage(Message message, boolean multiVote) {
        ArrayList<HashSet<Long>> votes = new ArrayList<>();

        MessageEmbed embed = message.getEmbeds().get(0);
        List<MessageEmbed.Field> field = embed.getFields();

        String topic = field.get(0).getValue();
        String choiceString = field.get(1).getValue();
        String[] choices = new String[choiceString.split("\n").length];

        for (int i = 0; i < choices.length; i++) {
            String choiceLine = choiceString.split("\n")[i];
            choices[i] = choiceLine.substring(choiceLine.split("\\|")[0].length() + 1);
        }

        for (int i = 0; i < choices.length; i++) {
            HashSet<Long> voteUsers = new HashSet<>();

            for (MessageReaction reaction : message.getReactions()) {
                if (EmojiUtil.equals(reaction.getEmoji(), Emojis.LETTERS[i])) {
                    List<User> users = reaction.retrieveUsers().complete();
                    users.forEach(user -> {
                        if (!user.isBot()) {
                            voteUsers.add(user.getIdLong());
                        }
                    });

                    break;
                }
            }

            votes.add(voteUsers);
        }

        boolean newVersion = true;
        String creatorIdString = DBStaticReactionMessages.getInstance().retrieve(message.getGuildIdLong())
                .get(message.getIdLong())
                .getSecondaryId();

        if (creatorIdString == null && embed.getFooter() != null && embed.getFooter().getText() != null) {
            String footerString = embed.getFooter().getText();
            if (footerString.contains("｜")) {
                newVersion = false;
                creatorIdString = footerString.split("｜")[0];
            } else if (footerString.contains(" ")) {
                newVersion = false;
                creatorIdString = footerString.split(" ")[0];
            }
        }

        long creatorId = 0L;
        if (creatorIdString != null && StringUtil.stringIsLong(creatorIdString)) {
            creatorId = Long.parseLong(creatorIdString);
        }

        return new VoteInfo(topic, choices, votes, creatorId, newVersion, multiVote);
    }

}
