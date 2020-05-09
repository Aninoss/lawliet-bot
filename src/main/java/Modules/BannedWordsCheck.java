package Modules;

import CommandSupporters.CommandManager;
import Commands.ModerationCategory.BannedWordsCommand;
import Commands.ModerationCategory.ModSettingsCommand;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.PermissionCheck;
import Core.TextManager;
import MySQL.Modules.BannedWords.BannedWordsBean;
import MySQL.Modules.BannedWords.DBBannedWords;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class BannedWordsCheck {

    final static Logger LOGGER = LoggerFactory.getLogger(BannedWordsCheck.class);

    public static boolean checkForBannedWordUsaqe(Server server, Message message) throws InterruptedException {
        String input = message.getContent();

        try {
            BannedWordsBean bannedWordsBean = DBBannedWords.getInstance().getBean(server.getId());
            if (bannedWordsBean.isActive() && stringContainsWord(input, new ArrayList<>(bannedWordsBean.getWords())) && !bannedWordsBean.getIgnoredUserIds().contains(message.getUserAuthor().get().getId()) && !PermissionCheck.hasAdminPermissions(server, message.getUserAuthor().get())) {
                boolean successful = true;
                User author = message.getUserAuthor().get();

                //Nachricht löschen
                try {
                    message.delete().get();
                } catch (ExecutionException e) {
                    LOGGER.error("Could not delete message", e);
                    successful = false;
                }

                //Poster informieren
                Locale locale = bannedWordsBean.getServerBean().getLocale();
                BannedWordsCommand bannedWordsCommand = new BannedWordsCommand();
                bannedWordsCommand.setLocale(locale);

                EmbedBuilder ebUser = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_content"), message.getContent(), true)
                        .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_successful_user"));
                try {
                    author.sendMessage(ebUser).get();
                } catch (ExecutionException e) {
                    //Ignore
                }

                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_content"), message.getContent(), true);
                if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_successful", author.getMentionTag()));
                else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_failed", author.getMentionTag()));

                for(User user: bannedWordsBean.getLogReceiverUserIds().transform(server::getMemberById, DiscordEntity::getId)) {
                    try {
                        user.sendMessage(eb).get();
                    } catch (ExecutionException e) {
                        //Ignore
                    }
                }

                eb = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_content"), message.getContent(), true);
                if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_successful", author.getMentionTag()));
                else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_failed", author.getMentionTag()));

                ModSettingsCommand.postLog(CommandManager.createCommandByClass(BannedWordsCommand.class, locale), eb, server);
                ModSettingsCommand.insertWarning(locale, server, author, DiscordApiCollection.getInstance().getYourself(), TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_title"));

                return true;
            }
        } catch (IllegalAccessException | InstantiationException | ExecutionException e) {
            LOGGER.error("Exception", e);
        }

        return false;
    }

    private static boolean stringContainsWord(String input, ArrayList<String> badWords) {
        input = translateString(input);
        for(String word: badWords) {
            if (input.contains(word)) return true;
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
        input = input.toLowerCase().replaceAll("[^a-zA-Z]", "");

        input = input.replace("\n","");

        return input;
    }

    private static int getLongestWordCount(ArrayList<String> badWords) {
        int length = 0;
        for(String str: badWords) {
            length = Math.max(length, str.length());
        }
        return length;
    }
}
