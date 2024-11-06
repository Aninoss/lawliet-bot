package commands.runnables.aitoyscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.mention.MentionList;
import core.utils.*;
import modules.textai.TextAI;
import modules.textai.TextAICache;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "imitate",
        emoji = "🔁",
        executableWithoutArgs = true,
        patreonRequired = true,
        enableCacheWipe = false,
        requiresFullMemberCache = true,
        aliases = {"impersonate"}
)
public class ImitateCommand extends Command {

    private static final TextAICache textAICache = new TextAICache();

    public ImitateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        FeatureLogger.inc(PremiumFeature.AI, event.getGuild().getIdLong());

        MentionList<Member> memberMentions = MentionUtil.getMembers(event.getGuild(), args, event.getRepliedMember());
        List<Member> members = memberMentions.getList();

        ArrayList<Message> tempMessageCache = new ArrayList<>();
        Member member = null;

        if (!args.equalsIgnoreCase("all") &&
                !args.equalsIgnoreCase("everyone")
        ) {
            member = members.isEmpty() ? event.getMember() : members.get(0);
        }

        String search = member != null ? StringUtil.escapeMarkdown(member.getEffectiveName()) : "**" + StringUtil.escapeMarkdown(event.getGuild().getName()) + "**";

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("wait", search, EmojiUtil.getLoadingEmojiMention(event.getMessageChannel())));
        drawMessage(eb).get();

        eb = getEmbed(event, member, 2, tempMessageCache);
        if (members.isEmpty()) {
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
        }

        drawMessage(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private EmbedBuilder getEmbed(CommandEvent event, Member member, int n, ArrayList<Message> tempMessageCache) {
        Guild guild = event.getGuild();
        TextAI textAI = new TextAI(n);
        TextAI.WordMap wordMap;
        if (member != null) {
            wordMap = textAICache.get(guild.getIdLong(), member.getIdLong(), n);
        } else {
            wordMap = textAICache.get(guild.getIdLong(), n);
        }

        if (wordMap.isEmpty()) {
            if (tempMessageCache.isEmpty()) {
                fetchMessages(guild, member, tempMessageCache);
            }

            if (tempMessageCache.isEmpty()) return getErrorEmbed();
            tempMessageCache.forEach(message -> processMessage(textAI, wordMap, message));
        }

        Optional<String> response = textAI.generateTextWithWordMap(wordMap, 900);
        if (response.isEmpty() && n > 1) {
            return getEmbed(event, member, n - 1, tempMessageCache);
        }

        if (response.isPresent()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setDescription(response.get());
            EmbedUtil.setFooter(eb, this);

            if (member != null) {
                EmbedUtil.setMemberAuthor(eb, member);
            } else {
                eb.setAuthor(guild.getName(), null, guild.getIconUrl());
            }

            return eb;
        } else {
            return getErrorEmbed();
        }
    }

    private EmbedBuilder getErrorEmbed() {
        return EmbedFactory.getEmbedError(
                this,
                getString("nomessage"),
                TextManager.getString(getLocale(), TextManager.GENERAL, "no_results")
        );
    }

    private void fetchMessages(Guild guild, Member member, ArrayList<Message> tempMessageCache) {
        final int REQUESTS = 80;

        ArrayList<GuildMessageChannel> channels = guild.getChannelCache().stream()
                .filter(channel -> channel instanceof GuildMessageChannel)
                .map(channel -> (GuildMessageChannel) channel)
                .sorted()
                .filter(channel -> !JDAUtil.channelIsNsfw(channel) &&
                        BotPermissionUtil.canReadHistory(channel) &&
                        (member == null || BotPermissionUtil.canWrite(member, channel)) &&
                        BotPermissionUtil.channelIsPublic(channel)
                ).limit(REQUESTS)
                .collect(Collectors.toCollection(ArrayList::new));

        int requestsPerPage = (int) Math.ceil(REQUESTS / (double) channels.size());

        for (GuildMessageChannel channel : channels) {
            MessageHistory messageHistory = channel.getHistory();
            for (int i = 0; i < requestsPerPage; i++) {
                List<Message> messages = messageHistory.retrievePast(100).complete();
                for (Message message : messages) {
                    if (member == null || message.getAuthor().getIdLong() == member.getIdLong()) {
                        tempMessageCache.add(message);
                    }
                }

                if (messages.size() < 100) {
                    break;
                }
            }
        }
    }

    private void processMessage(TextAI textAI, TextAI.WordMap wordMap, Message message) {
        String content = message.getContentRaw();
        if (content.isEmpty()) return;
        if (!content.contains(" ")) content += " ";

        String[] words = content.split(" ");
        if (StringUtil.stringIsLetters(words[0])) {
            textAI.extractModelToWordMap(content, wordMap);
        }
    }

}
