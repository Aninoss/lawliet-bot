package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.NavigationHelper;
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
import java.util.Arrays;
import java.util.List;

@CommandProperties(
    trigger = "bannedwords",
    botPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL,
    userPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.KICK_USER,
    emoji = "\uD83D\uDEA7️",
    thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/road-block-icon.png",
    executable = true
)
public class BannedWordsCommand extends Command implements onNavigationListener {

    private static final int MAX_WORDS = 20;

    private BannedWords bannedWords;
    private NavigationHelper<String> wordsNavigationHelper;

    public BannedWordsCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            bannedWords = DBServer.getBannedWordsFromServer(event.getServer().get());
            wordsNavigationHelper = new NavigationHelper<>(this, bannedWords.getWords(), String.class, MAX_WORDS);
            return Response.TRUE;
        }

        switch (state) {
            case 1:
                ArrayList<User> userIgnoredList = MentionFinder.getUsers(event.getMessage(), inputString).getList();
                if (userIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    bannedWords.setIgnoredUserIds(userIgnoredList);
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
                    bannedWords.setLogRecieverIds(logRecieverList);
                    DBServer.saveBannedWords(bannedWords);
                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
                String[] wordArray = inputString.replace("\n", " ").split(" ");
                List<String> wordList = Arrays.asList(wordArray);
                if (wordsNavigationHelper.addData(wordList, inputString, event.getMessage().getUserAuthor().get(), 0, word -> {}) == Response.TRUE) {
                    DBServer.saveBannedWords(bannedWords);
                    return Response.TRUE;
                } else return Response.FALSE;
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

                    case 4:
                        setState(4);
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
                if (i == -1) {
                    setState(0);
                    return true;
                }
                return false;

            case 4:
                if (wordsNavigationHelper.removeData(i, 0, word -> {})) {
                    DBServer.saveBannedWords(bannedWords);
                    return true;
                }
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
                       .addField(getString("state0_mignoredusers"), new ListGen<User>().getList(bannedWords.getIgnoredUserIds(), getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_mlogreciever"), new ListGen<User>().getList(bannedWords.getLogRecieverIds(), getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_mwords"), getWordsString(), true);

            case 1:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3: return wordsNavigationHelper.drawDataAdd(getString("state3_title"), getString("state3_description"));
            case 4: return wordsNavigationHelper.drawDataRemove(getString("state4_title"), getString("state4_description"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    private String getWordsString() throws IOException {
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
