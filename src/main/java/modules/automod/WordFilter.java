package modules.automod;

import commands.Command;
import commands.runnables.moderationcategory.WordFilterCommand;
import constants.Category;
import core.TextManager;
import mysql.modules.bannedwords.BannedWordsBean;
import mysql.modules.bannedwords.DBBannedWords;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class WordFilter extends AutoModAbstract {

    private final BannedWordsBean bannedWordsBean;

    public WordFilter(Message message) throws ExecutionException {
        super(message);
        bannedWordsBean = DBBannedWords.getInstance().getBean(message.getServer().get().getId());
    }

    @Override
    protected boolean withAutoActions(Message message, Locale locale) {
        return true;
    }

    @Override
    protected void designEmbed(Message message, Locale locale, EmbedBuilder eb) {
        eb.setDescription(TextManager.getString(locale, Category.MODERATION, "wordfilter_log", message.getUserAuthor().get().getDiscriminatedName()))
                .addField(TextManager.getString(locale, Category.MODERATION, "wordfilter_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                .addField(TextManager.getString(locale, Category.MODERATION, "wordfilter_log_content"), message.getContent(), true);

        bannedWordsBean.getLogReceiverUserIds().transform(message.getServer().get()::getMemberById, DiscordEntity::getId).forEach(user -> user.sendMessage(eb));
    }

    @Override
    protected Class<? extends Command> getCommandClass() {
        return WordFilterCommand.class;
    }

    @Override
    protected boolean checkCondition(Message message) {
        return bannedWordsBean.isActive() &&
                stringContainsWord(message.getContent(), new ArrayList<>(bannedWordsBean.getWords())) &&
                !bannedWordsBean.getIgnoredUserIds().contains(message.getUserAuthor().get().getId());
    }

    private boolean stringContainsWord(String input, ArrayList<String> badWords) {
        input = " " + translateString(input) + " ";
        for(String word: badWords) {
            if (input.contains(" " + word + " "))
                return true;
        }
        return false;
    }

    public static String translateString(String input) {
        input = input.replace("1","i");
        input = input.replace("!","i");
        input = input.replace("3","e");
        input = input.replace("4","a");
        input = input.replace("@","a");
        input = input.replace("5","s");
        input = input.replace("7","t");
        input = input.replace("0","o");
        input = input.replace("9","g");
        return input.toLowerCase()
                .replace("\n", " ")
                .replaceAll("[^\\p{IsLatin}\\p{IsCyrillic}\\p{IsArabic} ]", "");
    }

}
