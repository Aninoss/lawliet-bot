package core.cache;

import constants.LetterEmojis;
import core.utils.DiscordUtil;
import core.utils.StringUtil;
import modules.VoteInfo;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class VoteCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(VoteCache.class);

    private static final VoteCache ourInstance = new VoteCache();
    public static VoteCache getInstance() { return ourInstance; }
    private VoteCache() { }

    private final HashMap<Long, VoteInfo> voteCache = new HashMap<>();

    public void put(long messageId, VoteInfo voteInfo) {
        voteCache.put(messageId, voteInfo);
    }

    public Optional<VoteInfo> get(Message message, long userId, String emoji, boolean add) {
        VoteInfo voteInfo = voteCache.get(message.getId());

        if (voteInfo != null) {
            int i = -1;
            for (int j = 0; j < voteInfo.getSize(); j++) {
                if (emoji.equals(LetterEmojis.LETTERS[j])) {
                    i = j;
                    break;
                }
            }

            if ((i < 0 && !emoji.equals("❌")) || !voteInfo.isActive())
                return Optional.empty();

            if (i >= 0) {
                if (add)
                    voteInfo.addVote(i, userId);
                else
                    voteInfo.removeVote(i, userId);
            }
        } else {
            voteInfo = extractVoteInfoFromMessage(message);
            voteCache.put(message.getId(), voteInfo);
        }

        return Optional.of(voteInfo);
    }

    private VoteInfo extractVoteInfoFromMessage(Message message) {
        ArrayList<HashSet<Long>> votes = new ArrayList<>();

        Embed embed = message.getEmbeds().get(0);
        List<EmbedField> field = embed.getFields();

        String topic = field.get(0).getValue();
        String choiceString = field.get(1).getValue();
        String[] choices = new String[choiceString.split("\n").length];

        for(int i = 0; i < choices.length; i++) {
            String choiceLine = choiceString.split("\n")[i];
            choices[i] = choiceLine.split("\\|")[1];
        }

        for(int i = 0; i < choices.length; i++) {
            HashSet<Long> voteUsers = new HashSet<>();

            for (Reaction reaction: message.getReactions()) {
                if (DiscordUtil.emojiIsString(reaction.getEmoji(), LetterEmojis.LETTERS[i])) {
                    try {
                        reaction.getUsers().get().forEach(user -> {
                            if (!user.isBot()) {
                                voteUsers.add(user.getId());
                            }
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Exception", e);
                    }

                    break;
                }
            }

            votes.add(voteUsers);
        }

        long creatorId = -1;
        if (embed.getFooter().isPresent()) {
            Optional<String> footerStringOptional = embed.getFooter().get().getText();
            if (footerStringOptional.isPresent()) {
                String footerString = footerStringOptional.get();
                if (footerString.contains(" ")) {
                    String creatorIdString = footerString.split(" ")[0];
                    if (StringUtil.stringIsLong(creatorIdString)) {
                        creatorId = Long.parseLong(creatorIdString);
                    }
                }
            }

        }

        return new VoteInfo(topic, choices, votes, creatorId);
    }

}
