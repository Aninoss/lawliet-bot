package modules.automod;

import commands.Category;
import commands.Command;
import commands.runnables.moderationcategory.WordFilterCommand;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.WordFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.Locale;

public class WordFilter extends AutoModAbstract {

    private final WordFilterEntity wordFilterEntity;

    public WordFilter(Message message, GuildEntity guildEntity) {
        super(message, guildEntity);
        wordFilterEntity = guildEntity.getWordFilter();
    }

    @Override
    protected boolean withAutoActions(Message message, Locale locale) {
        return true;
    }

    @Override
    protected void designEmbed(Message message, Locale locale, EmbedBuilder eb) {
        eb.setDescription(TextManager.getString(locale, Category.MODERATION, "wordfilter_log", StringUtil.escapeMarkdown(message.getAuthor().getName())))
                .addField(TextManager.getString(locale, Category.MODERATION, "wordfilter_log_channel"), message.getChannel().getAsMention(), true)
                .addField(TextManager.getString(locale, Category.MODERATION, "wordfilter_log_content"), StringUtil.shortenString(message.getContentRaw(), 1024), true);

        for (Long userId : wordFilterEntity.getLogReceiverUserIds()) {
            if (userId != message.getGuild().getSelfMember().getIdLong()) {
                JDAUtil.openPrivateChannel(message.getJDA(), userId)
                        .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                        .queue();
            }
        }
    }

    @Override
    protected Class<? extends Command> getCommandClass() {
        return WordFilterCommand.class;
    }

    @Override
    protected boolean checkCondition(Message message) {
        return wordFilterEntity.getActive() &&
                stringContainsWord(message.getContentRaw(), new ArrayList<>(wordFilterEntity.getWords())) &&
                !wordFilterEntity.getExcludedMemberIds().contains(message.getAuthor().getIdLong()) &&
                !BotPermissionUtil.can(message.getMember(), Permission.ADMINISTRATOR);
    }

    private boolean stringContainsWord(String input, ArrayList<String> badWords) {
        input = " " + translateString(input) + " ";
        for (String word : badWords) {
            if (input.contains(" " + word + " ")) {
                return true;
            }
        }
        return false;
    }

    public static String translateString(String input) {
        return input.toLowerCase()
                .replace("1", "i")
                .replace("3", "e")
                .replace("4", "a")
                .replace("@", "a")
                .replace("5", "s")
                .replace("7", "t")
                .replace("0", "o")
                .replace("9", "g")
                .replace("\n", " ")
                .replaceAll("[^\\p{IsLatin}\\p{IsCyrillic}\\p{IsArabic} ]", "");
    }

}
