package Modules;

import CommandSupporters.CommandManager;
import Commands.ModerationCategory.BannedWordsCommand;
import Constants.Category;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.Utils.PermissionUtil;
import Core.TextManager;
import MySQL.Modules.BannedWords.BannedWordsBean;
import MySQL.Modules.BannedWords.DBBannedWords;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class BannedWordsCheck {

    public static boolean check(Message message) throws ExecutionException, InstantiationException, IllegalAccessException, InterruptedException, InvocationTargetException {
        Server server = message.getServer().get();
        String input = message.getContent();
        User author = message.getUserAuthor().get();
        BannedWordsBean bannedWordsBean = DBBannedWords.getInstance().getBean(server.getId());
        Locale locale = bannedWordsBean.getServerBean().getLocale();

        if (bannedWordsBean.isActive() &&
                stringContainsWord(input, new ArrayList<>(bannedWordsBean.getWords())) &&
                !bannedWordsBean.getIgnoredUserIds().contains(message.getUserAuthor().get().getId()) &&
                !PermissionUtil.hasAdminPermissions(server, message.getUserAuthor().get())
        ) {
            BannedWordsCommand bannedWordsCommand = (BannedWordsCommand) CommandManager.createCommandByClass(BannedWordsCommand.class, locale, bannedWordsBean.getServerBean().getPrefix());

            informMessageAuthor(bannedWordsCommand, locale, message, author);
            boolean successful = safeDeleteMessage(message);
            informLogReceivers(bannedWordsBean, bannedWordsCommand, locale, message, author, successful);

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                    .addField(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                    .addField(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_content"), message.getContent(), true);

            if (successful) eb.setDescription(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_successful", author.getMentionTag()));
            else eb.setDescription(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_failed", author.getMentionTag()));

            Mod.postLog(CommandManager.createCommandByClass(BannedWordsCommand.class, locale, bannedWordsBean.getServerBean().getPrefix()), eb, server);
            Mod.insertWarning(locale, server, author, DiscordApiCollection.getInstance().getYourself(), TextManager.getString(locale, Category.MODERATION, "bannedwords_title"), true);

            return false;
        }

        return true;
    }

    private static void informLogReceivers(BannedWordsBean bannedWordsBean, BannedWordsCommand bannedWordsCommand, Locale locale, Message message, User author, boolean successful) throws InterruptedException {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                .addField(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                .addField(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_content"), message.getContent(), true);
        if (successful) eb.setDescription(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_successful", author.getMentionTag()));
        else eb.setDescription(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_failed", author.getMentionTag()));

        for(User user: bannedWordsBean.getLogReceiverUserIds().transform(message.getServer().get()::getMemberById, DiscordEntity::getId)) {
            try {
                user.sendMessage(eb).get();
            } catch (ExecutionException e) {
                //Ignore
            }
        }
    }

    private static void informMessageAuthor(BannedWordsCommand bannedWordsCommand, Locale locale, Message message, User author) throws InterruptedException {
        EmbedBuilder ebUser = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                .addField(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                .addField(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_content"), message.getContent(), true)
                .setDescription(TextManager.getString(locale, Category.MODERATION, "bannedwords_log_successful_user"));
        try {
            author.sendMessage(ebUser).get();
        } catch (ExecutionException e) {
            //Ignore
        }
    }

    private static boolean safeDeleteMessage(Message message) throws InterruptedException {
        try {
            message.delete().get();
        } catch (ExecutionException e) {
            return false;
            //Ignore
        }
        return true;
    }

    private static boolean stringContainsWord(String input, ArrayList<String> badWords) {
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
        return input.toLowerCase().replaceAll("[^\\p{IsLatin}\\p{IsCyrillic}\\p{IsArabic}]", " ");
    }

    private static int getLongestWordCount(ArrayList<String> badWords) {
        int length = 0;
        for(String str: badWords) {
            length = Math.max(length, str.length());
        }
        return length;
    }

}
