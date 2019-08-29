package Commands.Moderation;

import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import General.*;
import General.BannedWords.BannedWords;
import General.Mention.MentionFinder;
import MySQL.DBServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;

public class BannedWordsCommand extends Command implements onNavigationListener {
    private BannedWords bannedWords;

    public BannedWordsCommand() {
        super();
        trigger = "bannedwords";
        privateUse = false;
        botPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL;
        userPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.KICK_USER;
        nsfw = false;
        withLoadingBar = false;
        emoji = "\uD83D\uDEA7️";
        thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/road-block-icon.png";
        executable = true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, boolean firstTime) throws Throwable {
        if (firstTime) {
            bannedWords = DBServer.getBannedWordsFromServer(event.getServer().get());
            return Response.TRUE;
        }

        switch (state) {
            case 1:
                ArrayList<User> userIgnoredList = MentionFinder.getUsers(event.getMessage(), inputString).getList();
                if (userIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    bannedWords.setIgnoredUser(userIgnoredList);
                    DBServer.saveBannedWords(bannedWords);
                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    state = 0;
                    return Response.TRUE;
                }

            case 2:
                ArrayList<User> logRecieverList = MentionFinder.getUsers(event.getMessage(), inputString).getList();
                if (logRecieverList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    bannedWords.setLogRecievers(logRecieverList);
                    DBServer.saveBannedWords(bannedWords);
                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    state = 0;
                    return Response.TRUE;
                }

            case 3:
                bannedWords.resetWords();
                if (inputString.length() > 0) {
                    String wordContainer = inputString.replace("\n", " ");
                    for(String word: wordContainer.split(" ")) {
                        bannedWords.addWord(word);
                    }
                }

                DBServer.saveBannedWords(bannedWords);
                setLog(LogStatus.SUCCESS, getString("wordsset"));
                state = 0;
                return Response.TRUE;
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        bannedWords.setActive(!bannedWords.isActive());
                        DBServer.saveBannedWords(bannedWords);
                        setLog(LogStatus.SUCCESS, getString("onoffset", !bannedWords.isActive()));
                        return true;

                    case 1:
                        state = 1;
                        return true;

                    case 2:
                        state = 2;
                        return true;

                    case 3:
                        state = 3;
                        return true;
                }
                return false;

            case 1:
                switch (i) {
                    case -1:
                        state = 0;
                        return true;

                    case 0:
                        bannedWords.resetIgnoredUser();
                        DBServer.saveBannedWords(bannedWords);
                        state = 0;
                        setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                        return true;
                }
                return false;

            case 2:
                switch (i) {
                    case -1:
                        state = 0;
                        return true;

                    case 0:
                        bannedWords.resetLogRecievers();
                        DBServer.saveBannedWords(bannedWords);
                        state = 0;
                        setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                        return true;
                }
                return false;

            case 3:
                switch (i) {
                    case -1:
                        state = 0;
                        return true;

                    case 0:
                        bannedWords.resetWords();
                        DBServer.saveBannedWords(bannedWords);
                        state = 0;
                        setLog(LogStatus.SUCCESS, getString("wordsset"));
                        return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api) throws Throwable {
        switch (state) {
            case 0:
                options = getString("state0_options").split("\n");
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                       .addField(getString("state0_menabled"), Tools.getOnOffForBoolean(locale, bannedWords.isActive()), true)
                       .addField(getString("state0_mignoredusers"),ListGen.getUserList(locale, bannedWords.getIgnoredUser()), true)
                       .addField(getString("state0_mlogreciever"),ListGen.getUserList(locale, bannedWords.getLogRecievers()), true)
                       .addField(getString("state0_mwords"), getWords(), true);

            case 1:
                options = new String[]{getString("empty")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                options = new String[]{getString("empty")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3:
                options = new String[]{getString("empty")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {
    }

    @Override
    public int getMaxReactionNumber() {
        return 4;
    }

    private String getWords() throws Throwable {
        ArrayList<String> words = bannedWords.getWords();
        if (words.size() == 0) {
            return TextManager.getString(locale, TextManager.GENERAL, "notset");
        } else {
            StringBuilder sb = new StringBuilder();

            for(String word: words) {
                sb.append(" ").append(word);
            }

            return sb.toString();
        }
    }
}
