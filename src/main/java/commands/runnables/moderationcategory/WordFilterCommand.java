package commands.runnables.moderationcategory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.atomicassets.MentionableAtomicAsset;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.automod.WordFilter;
import mysql.modules.bannedwords.BannedWordsData;
import mysql.modules.bannedwords.DBBannedWords;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "wordfilter",
        botGuildPermissions = Permission.MESSAGE_MANAGE,
        userGuildPermissions = { Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS },
        emoji = "️🚧️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        usesExtEmotes = true,
        aliases = { "wordsfilter", "badwordfilter", "badwordsfilter", "bannedwords" }
)
public class WordFilterCommand extends NavigationAbstract {

    private static final int MAX_WORDS = 40;
    private static final int MAX_LETTERS = 20;

    private BannedWordsData bannedWordsBean;
    private NavigationHelper<String> wordsNavigationHelper;
    private CustomObservableList<AtomicMember> ignoredUsers;
    private CustomObservableList<AtomicMember> logReceivers;

    public WordFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        bannedWordsBean = DBBannedWords.getInstance().retrieve(event.getGuild().getIdLong());
        ignoredUsers = AtomicMember.transformIdList(event.getGuild(), bannedWordsBean.getIgnoredUserIds());
        logReceivers = AtomicMember.transformIdList(event.getGuild(), bannedWordsBean.getLogReceiverUserIds());
        wordsNavigationHelper = new NavigationHelper<>(this, bannedWordsBean.getWords(), String.class, MAX_WORDS);
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        switch (state) {
            case 1:
                List<Member> memberIgnoredList = MentionUtil.getMembers(event.getGuild(), input, null).getList();
                if (memberIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else {
                    ignoredUsers.clear();
                    ignoredUsers.addAll(memberIgnoredList.stream().map(AtomicMember::new).collect(Collectors.toList()));
                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            case 2:
                List<Member> logRecieverList = MentionUtil.getMembers(event.getGuild(), input, null).getList();
                if (logRecieverList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else {
                    logReceivers.clear();
                    logReceivers.addAll(logRecieverList.stream().map(AtomicMember::new).collect(Collectors.toList()));
                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            case 3:
                String[] wordArray = WordFilter.translateString(input).split(" ");
                List<String> wordList = Arrays
                        .stream(wordArray)
                        .filter(str -> str.length() > 0)
                        .map(str -> str.substring(0, Math.min(MAX_LETTERS, str.length())))
                        .collect(Collectors.toList());
                return wordsNavigationHelper.addData(wordList, input, event.getMember(), 0);

            default:
                return null;
        }
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
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
    public EmbedBuilder draw(Member member, int state) {
        switch (state) {
            case 0:
                setComponents(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), bannedWordsBean.isActive()), true)
                        .addField(getString("state0_mignoredusers"), new ListGen<AtomicMember>().getList(ignoredUsers, getLocale(), MentionableAtomicAsset::getAsMention), true)
                        .addField(getString("state0_mlogreciever"), new ListGen<AtomicMember>().getList(logReceivers, getLocale(), MentionableAtomicAsset::getAsMention), true)
                        .addField(getString("state0_mwords"), getWordsString(), true);

            case 1:
                setComponents(getString("empty"));
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setComponents(getString("empty"));
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                return wordsNavigationHelper.drawDataAdd(getString("state3_title"), getString("state3_description"));
            case 4:
                return wordsNavigationHelper.drawDataRemove(getString("state4_title"), getString("state4_description"));

            default:
                return null;
        }
    }

    private String getWordsString() {
        List<String> words = bannedWordsBean.getWords();
        if (words.size() == 0) {
            return TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        } else {
            StringBuilder sb = new StringBuilder();

            for (String word : words) {
                sb.append(" ").append(word);
            }

            return sb.toString();
        }
    }

}
