package Commands.Moderation;

import CommandListeners.CommandProperties;
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

import java.io.IOException;
import java.util.ArrayList;

@CommandProperties(
    trigger = "bannedwords",
    botPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL,
    userPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.KICK_USER,
    emoji = "\uD83D\uDEA7️",
    thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/road-block-icon.png",
    executable = true
)
public class BannedWordsCommand extends Command implements onNavigationListener {

    private BannedWords bannedWords;

    public BannedWordsCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            bannedWords = DBServer.getBannedWordsFromServer(event.getServer().get());
            return Response.TRUE;
        }

        switch (state) {
            case 1:
                ArrayList<User> userIgnoredList = MentionFinder.getUsers(event.getMessage(), inputString).getList();
                if (userIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    bannedWords.setIgnoredUser(userIgnoredList);
                    DBServer.saveBannedWords(bannedWords);
                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 2:
                ArrayList<User> logRecieverList = MentionFinder.getUsers(event.getMessage(), inputString).getList();
                if (logRecieverList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    bannedWords.setLogRecievers(logRecieverList);
                    DBServer.saveBannedWords(bannedWords);
                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    setState(0);
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
                setState(0);
                return Response.TRUE;
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
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
                        setState(1);
                        return true;

                    case 2:
                        setState(2);
                        return true;

                    case 3:
                        setState(3);
                        return true;
                }
                return false;

            case 1:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        bannedWords.resetIgnoredUser();
                        DBServer.saveBannedWords(bannedWords);
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                        return true;
                }
                return false;

            case 2:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        bannedWords.resetLogRecievers();
                        DBServer.saveBannedWords(bannedWords);
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                        return true;
                }
                return false;

            case 3:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        bannedWords.resetWords();
                        DBServer.saveBannedWords(bannedWords);
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("wordsset"));
                        return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                       .addField(getString("state0_menabled"), Tools.getOnOffForBoolean(getLocale(), bannedWords.isActive()), true)
                       .addField(getString("state0_mignoredusers"), new ListGen<User>().getList(bannedWords.getIgnoredUser(), getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_mlogreciever"), new ListGen<User>().getList(bannedWords.getLogRecievers(), getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_mwords"), getWords(), true);

            case 1:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[]{getString("empty")});
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

    private String getWords() throws IOException {
        ArrayList<String> words = bannedWords.getWords();
        if (words.size() == 0) {
            return TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        } else {
            StringBuilder sb = new StringBuilder();

            for(String word: words) {
                sb.append(" ").append(word);
            }

            return sb.toString();
        }
    }
}
