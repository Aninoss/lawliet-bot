package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;
import commands.Command;
import core.EmbedFactory;
import core.mention.MentionList;
import core.utils.MentionUtil;
import core.TextManager;
import core.utils.PermissionUtil;
import core.utils.StringUtil;
import modules.textai.TextAI;
import modules.textai.TextAICache;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "imitate",
        emoji = "\uD83D\uDD01",
        executableWithoutArgs = true,
        patreonRequired = true,
        turnOffTimeout = true,
        aliases = { "impersonate" }
)
public class ImitateCommand extends Command {

    public ImitateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        MentionList<User> userMentions = MentionUtil.getUsers(event.getMessage(), followedString);
        ArrayList<User> users = userMentions.getList();

        ArrayList<Message> tempMessageCache = new ArrayList<>();
        User user = null;

        if (!followedString.equalsIgnoreCase("all") &&
                !followedString.equalsIgnoreCase("everyone")
        )
            user = users.isEmpty() ? event.getMessageAuthor().asUser().get() : users.get(0);

        String search = user != null ? user.getMentionTag() : "**" + StringUtil.escapeMarkdown(event.getServer().get().getName()) + "**";

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("wait", search, StringUtil.getLoadingReaction(event.getServerTextChannel().get())));
        Message message = event.getChannel().sendMessage(eb).get();

        eb = getEmbed(event.getServer().get(), user, 2, tempMessageCache);
        if (users.isEmpty()) eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));

        message.edit(eb).get();

        return true;
    }

    private EmbedBuilder getEmbed(Server server, User user, int n, ArrayList<Message> tempMessageCache) throws ExecutionException, InterruptedException {
        TextAI textAI = new TextAI(n);
        TextAI.WordMap wordMap;
        if (user != null) wordMap = TextAICache.getInstance().get(server.getId(), user.getId(), n);
        else wordMap = TextAICache.getInstance().get(server.getId(), n);

        if (wordMap.isEmpty()) {
            if (tempMessageCache.isEmpty())
                fetchMessages(server, user, tempMessageCache);

            if (tempMessageCache.isEmpty()) return getErrorEmbed();
            tempMessageCache.forEach(message -> processMessage(textAI, wordMap, message));
        }

        Optional<String> response = textAI.generateTextWithWordMap(wordMap,900);
        if (!response.isPresent() && n > 1) {
            return getEmbed(server, user, n - 1, tempMessageCache);
        }

        if (response.isPresent()) {
            EmbedBuilder eb = EmbedFactory.getEmbed()
                    .setDescription(response.get());

            if (user != null) eb.setAuthor(user);
            else eb.setAuthor(server.getName(), "", server.getIcon().map(icon -> icon.getUrl().toString()).orElse(""));

            return eb;
        } else {
            return getErrorEmbed();
        }
    }

    private EmbedBuilder getErrorEmbed() {
        return EmbedFactory.getCommandEmbedError(this,
                getString("nomessage"),
                TextManager.getString(getLocale(), TextManager.GENERAL, "no_results")
        );
    }

    private void fetchMessages(Server server, User user, ArrayList<Message> tempMessageCache) throws ExecutionException, InterruptedException {
        final int REQUESTS = 80;

        ArrayList<ServerTextChannel> channels = server.getTextChannels().stream()
                .filter(channel -> !channel.isNsfw() &&
                        channel.canYouSee() &&
                        channel.canYouReadMessageHistory() &&
                        (user == null || channel.canWrite(user)) &&
                        PermissionUtil.channelIsPublic(channel)
                ).limit(REQUESTS)
                .collect(Collectors.toCollection(ArrayList::new));

        int requestsPerPage = (int) Math.ceil(REQUESTS / (double) channels.size());

        for (ServerTextChannel channel : channels) {
            Message startMessage = null;
            for (int i = 0; i < requestsPerPage; i++) {
                MessageSet messageSet;
                if (startMessage == null) messageSet = channel.getMessages(100).get();
                else messageSet = startMessage.getMessagesBefore(100).get();

                for (Message message : messageSet.descendingSet()) {
                    if (user == null || message.getAuthor().getId() == user.getId()) {
                        tempMessageCache.add(message);
                        startMessage = message;
                    }
                }

                if (messageSet.size() < 100) break;
            }
        }
    }

    private void processMessage(TextAI textAI, TextAI.WordMap wordMap, Message message) {
        String content = message.getContent();
        if (content.isEmpty()) return;
        if (!content.contains(" ")) content += " ";

        String[] words = content.split(" ");
        if (StringUtil.stringIsLetters(words[0])) {
            textAI.extractModelToWordMap(content, wordMap);
        }
    }

}
