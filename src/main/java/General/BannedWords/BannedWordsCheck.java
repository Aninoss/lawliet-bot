package General.BannedWords;

import Commands.Moderation.BannedWordsCommand;
import Constants.Permission;
import General.*;
import MySQL.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class BannedWordsCheck {

    public static boolean checkForBannedWordUsaqe(Server server, Message message) {
        String input = message.getContent();

        try {
            BannedWords bannedWords = DBServer.getBannedWordsFromServer(server);
            if (bannedWords.isActive() && stringContainsWord(input, bannedWords.getWords()) && !bannedWords.getIgnoredUser().contains(message.getUserAuthor().get()) && !Tools.userHasAdminPermissions(server, message.getUserAuthor().get())) {
                boolean successful = true;
                User author = message.getUserAuthor().get();

                //Nachricht löschen
                try {
                    message.delete().get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    successful = false;
                    //Ignore
                }

                //Poster informieren
                Locale locale = DBServer.getServerLocale(server);
                BannedWordsCommand bannedWordsCommand = new BannedWordsCommand();
                bannedWordsCommand.setLocale(locale);

                EmbedBuilder ebUser = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_content"), message.getContent(), true)
                        .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_successful_user"));
                try {
                    author.sendMessage(ebUser).get();
                } catch (InterruptedException | ExecutionException e) {
                    //Ignore
                }

                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                        .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_content"), message.getContent(), true);
                if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_successful", author.getMentionTag()));
                else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_failed", author.getMentionTag()));

                for(User user: bannedWords.getLogRecievers()) {
                    try {
                        user.sendMessage(eb).get();
                    } catch (InterruptedException | ExecutionException e) {
                        //Ignore
                    }
                }

                ModerationStatus moderationStatus = DBServer.getModerationFromServer(server);
                if (moderationStatus.getChannel() != null && PermissionCheckRuntime.getInstance().botHasPermission(locale, "bannedwords", moderationStatus.getChannel(), Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS)) {
                    eb = EmbedFactory.getCommandEmbedStandard(bannedWordsCommand)
                            .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                            .addField(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_content"), message.getContent(), true);
                    if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_successful", author.getMentionTag()));
                    else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_log_failed", author.getMentionTag()));

                    moderationStatus.getChannel().sendMessage(eb).get();
                }

                DBServer.insertWarning(server, author, DiscordApiCollection.getInstance().getYourself(), TextManager.getString(locale, TextManager.COMMANDS, "bannedwords_title"));

                return true;
            }
        } catch (IOException | ExecutionException | SQLException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static boolean stringContainsWord(String input, ArrayList<String> badWords) {
        input = translateString(input);
        for(String word: badWords) {
            if (input.contains(word)) return true;
        }
        return false;

        /*for(int start = 0; start < input.length(); start++) {
            for(int offset = 1; offset < (input.length()+1 - start) && offset < getLongestWordCount(badWords); offset++)  {
                String wordToCheck = input.substring(start, start + offset);
                if(badWords.contains(wordToCheck)) {
                    return true;
                }
            }
        }

        return false;*/
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
