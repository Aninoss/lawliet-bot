package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.utils.CollectionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import kotlin.Pair;
import modules.automod.WordFilter;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.WordFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

    public static int MAX_IGNORED_USERS = 100;
    public static int MAX_LOG_RECEIVERS = 10;

    public static final int MAX_WORDS = 40;
    public static final int MAX_LETTERS = 20;

    private NavigationHelper<String> wordsNavigationHelper;

    public WordFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        wordsNavigationHelper = new NavigationHelper<>(this, guildEntity -> guildEntity.getWordFilter().getWords(), String.class, MAX_WORDS);
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        WordFilterEntity wordFilter = getGuildEntity().getWordFilter();

        switch (state) {
            case 1:
                List<Member> memberIgnoredList = MentionUtil.getMembers(event.getGuild(), input, null).getList();
                if (memberIgnoredList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else if (memberIgnoredList.size() > MAX_IGNORED_USERS) {
                    setLog(LogStatus.FAILURE, getString("toomanyignoredusers", StringUtil.numToString(MAX_IGNORED_USERS)));
                    return MessageInputResponse.FAILED;
                } else {
                    List<Long> newMemberIds = memberIgnoredList.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
                    Pair<List<String>, List<String>> addRemoveLists = BotLogEntity.oldNewToAddRemove(wordFilter.getExcludedMemberIds(), newMemberIds);

                    wordFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WORD_FILTER_EXCLUDED_MEMBERS, event.getMember(), addRemoveLists.getFirst(), addRemoveLists.getSecond());
                    CollectionUtil.replace(wordFilter.getExcludedMemberIds(), newMemberIds);
                    wordFilter.commitTransaction();

                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            case 2:
                List<Member> logRecieverList = MentionUtil.getMembers(event.getGuild(), input, null).getList();
                if (logRecieverList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else if (logRecieverList.size() > MAX_LOG_RECEIVERS) {
                    setLog(LogStatus.FAILURE, getString("toomanylogreceivers", StringUtil.numToString(MAX_LOG_RECEIVERS)));
                    return MessageInputResponse.FAILED;
                } else {
                    List<Long> newMemberIds = logRecieverList.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
                    Pair<List<String>, List<String>> addRemoveLists = BotLogEntity.oldNewToAddRemove(wordFilter.getLogReceiverUserIds(), newMemberIds);

                    wordFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WORD_FILTER_LOG_RECEIVERS, event.getMember(), addRemoveLists.getFirst(), addRemoveLists.getSecond());
                    CollectionUtil.replace(wordFilter.getLogReceiverUserIds(), newMemberIds);
                    wordFilter.commitTransaction();

                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            case 3:
                String[] wordArray = WordFilter.translateString(input).split(" ");
                List<String> wordList = Arrays.stream(wordArray)
                        .filter(str -> !str.isEmpty())
                        .map(str -> str.substring(0, Math.min(MAX_LETTERS, str.length())))
                        .collect(Collectors.toList());
                return wordsNavigationHelper.addData(wordList, input, event.getMember(), 0, BotLogEntity.Event.WORD_FILTER_WORDS);

            default:
                return null;
        }
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        WordFilterEntity wordFilter = getGuildEntity().getWordFilter();

        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
                        return false;

                    case 0:
                        wordFilter.beginTransaction();
                        wordFilter.setActive(!wordFilter.getActive());
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WORD_FILTER_ACTIVE, event.getMember(), null, wordFilter.getActive());
                        wordFilter.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("onoffset", !wordFilter.getActive()));
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
                    wordFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WORD_FILTER_EXCLUDED_MEMBERS, event.getMember(), null, wordFilter.getExcludedMemberIds());
                    wordFilter.getExcludedMemberIds().clear();
                    wordFilter.commitTransaction();

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
                    wordFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WORD_FILTER_LOG_RECEIVERS, event.getMember(), null, wordFilter.getLogReceiverUserIds());
                    wordFilter.getLogReceiverUserIds().clear();
                    wordFilter.commitTransaction();

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
                return wordsNavigationHelper.removeData(i, event.getMember(), 0, BotLogEntity.Event.WORD_FILTER_WORDS);

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        WordFilterEntity wordFilter = getGuildEntity().getWordFilter();

        switch (state) {
            case 0:
                setComponents(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), wordFilter.getActive()), true)
                        .addField(getString("state0_mignoredusers"), new ListGen<AtomicMember>().getList(wordFilter.getExcludedMembers(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                        .addField(getString("state0_mlogreciever"), new ListGen<AtomicMember>().getList(wordFilter.getLogReceivers(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                        .addField(getString("state0_mwords"), getWordsString(wordFilter.getWords()), true);

            case 1:
                setComponents(getString("empty"));
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setComponents(getString("empty"));
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                return wordsNavigationHelper.drawDataAdd(getString("state3_title"), getString("state3_description"));
            case 4:
                return wordsNavigationHelper.drawDataRemove(getString("state4_title"), getString("state4_description"), getLocale());

            default:
                return null;
        }
    }

    private String getWordsString(List<String> words) {
        if (words.size() == 0) {
            return TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        } else {
            StringBuilder sb = new StringBuilder();

            for (String word : words) {
                sb.append(" ").append("`").append(word).append("`");
            }

            return sb.toString();
        }
    }

}
