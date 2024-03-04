package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.atomicassets.AtomicTextChannel;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import kotlin.Pair;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.InviteFilterEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "invitefilter",
        botGuildPermissions = Permission.MESSAGE_MANAGE,
        userGuildPermissions = { Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS },
        emoji = "✉️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        usesExtEmotes = true,
        aliases = { "invitesfilter", "spblock", "inviteblock", "spfilter", "invitesblock" }
)
public class InviteFilterCommand extends NavigationAbstract {

    public static int MAX_IGNORED_USERS = 100;
    public static int MAX_IGNORED_CHANNELS = 100;
    public static int MAX_LOG_RECEIVERS = 10;

    public InviteFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        InviteFilterEntity inviteFilter = getGuildEntity().getInviteFilter();

        switch (state) {
            case 1:
                List<Member> userIgnoredList = MentionUtil.getMembers(event.getGuild(), input, null).getList();
                if (userIgnoredList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else if (userIgnoredList.size() > MAX_IGNORED_USERS) {
                    setLog(LogStatus.FAILURE, getString("toomanyignoredusers", StringUtil.numToString(MAX_IGNORED_USERS)));
                    return MessageInputResponse.FAILED;
                } else {
                    List<Long> newMemberIds = userIgnoredList.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
                    Pair<List<String>, List<String>> addRemoveLists = BotLogEntity.oldNewToAddRemove(inviteFilter.getExcludedMemberIds(), newMemberIds);

                    inviteFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_EXCLUDED_MEMBERS, event.getMember(), addRemoveLists.getFirst(), addRemoveLists.getSecond());
                    inviteFilter.setExcludedMemberIds(newMemberIds);
                    inviteFilter.commitTransaction();

                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            case 2:
                List<TextChannel> channelIgnoredList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
                if (channelIgnoredList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else if (channelIgnoredList.size() > MAX_IGNORED_CHANNELS) {
                    setLog(LogStatus.FAILURE, getString("toomanyignoredchannels", StringUtil.numToString(MAX_IGNORED_CHANNELS)));
                    return MessageInputResponse.FAILED;
                } else {
                    List<Long> newChannelIds = channelIgnoredList.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
                    Pair<List<String>, List<String>> addRemoveLists = BotLogEntity.oldNewToAddRemove(inviteFilter.getExcludedChannelIds(), newChannelIds);

                    inviteFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_EXCLUDED_CHANNELS, event.getMember(), addRemoveLists.getFirst(), addRemoveLists.getSecond());
                    inviteFilter.setExcludedChannelIds(newChannelIds);
                    inviteFilter.commitTransaction();

                    setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            case 3:
                List<Member> logRecieverList = MentionUtil.getMembers(event.getGuild(), input, null).getList();
                if (logRecieverList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else if (logRecieverList.size() > MAX_LOG_RECEIVERS) {
                    setLog(LogStatus.FAILURE, getString("toomanylogreceivers", StringUtil.numToString(MAX_LOG_RECEIVERS)));
                    return MessageInputResponse.FAILED;
                } else {
                    List<Long> newMemberIds = logRecieverList.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
                    Pair<List<String>, List<String>> addRemoveLists = BotLogEntity.oldNewToAddRemove(inviteFilter.getLogReceiverUserIds(), newMemberIds);

                    inviteFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_LOG_RECEIVERS, event.getMember(), addRemoveLists.getFirst(), addRemoveLists.getSecond());
                    inviteFilter.setLogReceiverUserIds(newMemberIds);
                    inviteFilter.commitTransaction();

                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            default:
                return null;
        }
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        InviteFilterEntity inviteFilter = getGuildEntity().getInviteFilter();

        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
                        return false;

                    case 0:
                        inviteFilter.beginTransaction();
                        inviteFilter.setActive(!inviteFilter.getActive());
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_ACTIVE, event.getMember(), null, inviteFilter.getActive());
                        inviteFilter.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("onoffset", !inviteFilter.getActive()));
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

                    default:
                        return false;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i == 0) {
                    inviteFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_EXCLUDED_MEMBERS, event.getMember(), null, inviteFilter.getExcludedMemberIds());
                    inviteFilter.setExcludedMemberIds(Collections.emptyList());
                    inviteFilter.commitTransaction();

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
                    inviteFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_EXCLUDED_CHANNELS, event.getMember(), null, inviteFilter.getExcludedChannelIds());
                    inviteFilter.setExcludedChannelIds(Collections.emptyList());
                    inviteFilter.commitTransaction();

                    setState(0);
                    setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                    return true;
                }
                return false;

            case 3:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i == 0) {
                    inviteFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_LOG_RECEIVERS, event.getMember(), null, inviteFilter.getLogReceiverUserIds());
                    inviteFilter.setLogReceiverUserIds(Collections.emptyList());
                    inviteFilter.commitTransaction();

                    setState(0);
                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    return true;
                }
                return false;

            case 4:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i <= 2) {
                    InviteFilterEntity.Action newAction = InviteFilterEntity.Action.values()[i];

                    inviteFilter.beginTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.INVITE_FILTER_ACTION, event.getMember(), inviteFilter.getAction(), newAction);
                    inviteFilter.setAction(newAction);
                    inviteFilter.commitTransaction();

                    setState(0);
                    setLog(LogStatus.SUCCESS, getString("actionset"));
                    return true;
                }
                return false;

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        InviteFilterEntity inviteFilter = getGuildEntity().getInviteFilter();

        switch (state) {
            case 0:
                Locale locale = getLocale();
                setComponents(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), inviteFilter.getActive()), true)
                        .addField(getString("state0_mignoredusers"), new ListGen<AtomicMember>().getList(inviteFilter.getExcludedMembers(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                        .addField(getString("state0_mignoredchannels"), new ListGen<AtomicTextChannel>().getList(inviteFilter.getExcludedChannels(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                        .addField(getString("state0_mlogreciever"), new ListGen<AtomicMember>().getList(inviteFilter.getLogReceivers(), getLocale(), m -> m.getPrefixedNameInField(locale)), true)
                        .addField(getString("state0_maction"), getString("state0_mactionlist").split("\n")[inviteFilter.getAction().ordinal()], true);

            case 1:
                setComponents(getString("empty"));
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setComponents(getString("empty"));
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setComponents(getString("empty"));
                return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setComponents(getString("state0_mactionlist").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));

            default:
                return null;
        }
    }

}
