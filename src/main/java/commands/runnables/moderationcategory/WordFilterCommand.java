package commands.runnables.moderationcategory;

import commands.Command;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListenerOld;
import constants.LogStatus;
import constants.PermissionDeprecated;
import constants.Response;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.automod.WordFilter;
import mysql.modules.bannedwords.BannedWordsBean;
import mysql.modules.bannedwords.DBBannedWords;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "wordfilter",
        botPermissions = PermissionDeprecated.MANAGE_MESSAGES,
        userPermissions = PermissionDeprecated.MANAGE_MESSAGES | PermissionDeprecated.KICK_MEMBERS,
        emoji = "️🚧️",
        executableWithoutArgs = true,
        aliases = { "wordsfilter", "badwordfilter", "badwordsfilter", "bannedwords" }
)
public class WordFilterCommand extends Command implements OnNavigationListenerOld {

    private static final int MAX_WORDS = 20;
    private static final int MAX_LETTERS = 20;

    private BannedWordsBean bannedWordsBean;
    private NavigationHelper<String> wordsNavigationHelper;
    private CustomObservableList<User> ignoredUsers, logReceivers;

    public WordFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        bannedWordsBean = DBBannedWords.getInstance().retrieve(event.getServer().get().getId());
        ignoredUsers = bannedWordsBean.getIgnoredUserIds().transform(userId -> event.getServer().get().getMemberById(userId), DiscordEntity::getId);
        logReceivers = bannedWordsBean.getLogReceiverUserIds().transform(userId -> event.getServer().get().getMemberById(userId), DiscordEntity::getId);
        wordsNavigationHelper = new NavigationHelper<>(this, bannedWordsBean.getWords(), String.class, MAX_WORDS);
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            case 1:
                ArrayList<User> userIgnoredList = MentionUtil.getMembers(event.getMessage(), inputString).getList();
                if (userIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), inputString));
                    return Response.FALSE;
                } else {
                    ignoredUsers.clear();
                    ignoredUsers.addAll(userIgnoredList);
                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 2:
                ArrayList<User> logRecieverList = MentionUtil.getMembers(event.getMessage(), inputString).getList();
                if (logRecieverList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), inputString));
                    return Response.FALSE;
                } else {
                    logReceivers.clear();
                    logReceivers.addAll(logRecieverList);
                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
                String[] wordArray = WordFilter.translateString(inputString).split(" ");
                List<String> wordList = Arrays
                        .stream(wordArray)
                        .filter(str -> str.length() > 0)
                        .map(str -> str.substring(0, Math.min(MAX_LETTERS, str.length())))
                        .collect(Collectors.toList());
                return wordsNavigationHelper.addData(wordList, inputString, event.getMessage().getUserAuthor().get(), 0);

            default:
                return null;
        }
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        bannedWordsBean.toggleActive();
                        setLog(LogStatus.SUCCESS, getString("onoffset", !bannedWordsBean.isActive()));
                        return true;

                    case 1:
                        setState(1);
                        return true;

                    case 2:
                        setState(2);
                        return true;

                    case 3:
                        wordsNavigationHelper.startDataAdd(3);
                        return true;

                    case 4:
                        wordsNavigationHelper.startDataRemove(4);
                        return true;

                    default:
                        return false;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i == 0) {
                    ignoredUsers.clear();
                    setState(0);
                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    return true;
                }
                return false;

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i == 0) {
                    logReceivers.clear();
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
                return wordsNavigationHelper.removeData(i, 0);

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                       .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getLocale(), bannedWordsBean.isActive()), true)
                       .addField(getString("state0_mignoredusers"), new ListGen<User>().getList(ignoredUsers, getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_mlogreciever"), new ListGen<User>().getList(logReceivers, getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_mwords"), getWordsString(), true);

            case 1:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3: return wordsNavigationHelper.drawDataAdd(getString("state3_title"), getString("state3_description"));
            case 4: return wordsNavigationHelper.drawDataRemove(getString("state4_title"), getString("state4_description"));

            default:
                return null;
        }
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    private String getWordsString() {
        List<String> words = bannedWordsBean.getWords();
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
