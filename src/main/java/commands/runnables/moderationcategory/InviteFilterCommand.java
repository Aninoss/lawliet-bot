package commands.runnables.moderationcategory;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Response;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.atomicassets.AtomicTextChannel;
import core.atomicassets.MentionableAtomicAsset;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.spblock.DBSPBlock;
import mysql.modules.spblock.SPBlockData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "invitefilter",
        botGuildPermissions = Permission.MESSAGE_MANAGE,
        userGuildPermissions = { Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS },
        emoji = "✉️",
        executableWithoutArgs = true,
        aliases = { "invitesfilter", "spblock", "inviteblock", "spfilter", "invitesblock" }
)
public class InviteFilterCommand extends NavigationAbstract {

    private SPBlockData spBlockBean;
    private CustomObservableList<AtomicMember> ignoredUsers;
    private CustomObservableList<AtomicMember> logReceivers;
    private CustomObservableList<AtomicTextChannel> ignoredChannels;

    public InviteFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        spBlockBean = DBSPBlock.getInstance().retrieve(event.getGuild().getIdLong());
        ignoredUsers = AtomicMember.transformIdList(event.getGuild(), spBlockBean.getIgnoredUserIds());
        logReceivers = AtomicMember.transformIdList(event.getGuild(), spBlockBean.getLogReceiverUserIds());
        ignoredChannels = AtomicTextChannel.transformIdList(event.getGuild(), spBlockBean.getIgnoredChannelIds());
        registerNavigationListener();
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        switch (state) {
            case 1:
                List<Member> userIgnoredList = MentionUtil.getMembers(event.getMessage(), input).getList();
                if (userIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    ignoredUsers.clear();
                    ignoredUsers.addAll(userIgnoredList.stream().map(AtomicMember::new).collect(Collectors.toList()));
                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 2:
                List<TextChannel> channelIgnoredList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
                if (channelIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    ignoredChannels.clear();
                    ignoredChannels.addAll(channelIgnoredList.stream().map(AtomicTextChannel::new).collect(Collectors.toList()));
                    setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
                List<Member> logRecieverList = MentionUtil.getMembers(event.getMessage(), input).getList();
                if (logRecieverList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    logReceivers.clear();
                    logReceivers.addAll(logRecieverList.stream().map(AtomicMember::new).collect(Collectors.toList()));

                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    setState(0);
                    return Response.TRUE;
                }

            default:
                return null;
        }
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithButtonMessage();
                        return false;

                    case 0:
                        spBlockBean.toggleActive();
                        setLog(LogStatus.SUCCESS, getString("onoffset", !spBlockBean.isActive()));
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
                    ignoredChannels.clear();
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
                    logReceivers.clear();
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
                    spBlockBean.setAction(SPBlockData.ActionList.values()[i]);
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
    public EmbedBuilder draw(int state) {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getLocale(), spBlockBean.isActive()), true)
                        .addField(getString("state0_mignoredusers"), new ListGen<AtomicMember>().getList(ignoredUsers, getLocale(), MentionableAtomicAsset::getAsMention), true)
                        .addField(getString("state0_mignoredchannels"), new ListGen<AtomicTextChannel>().getList(ignoredChannels, getLocale(), MentionableAtomicAsset::getAsMention), true)
                        .addField(getString("state0_mlogreciever"), new ListGen<AtomicMember>().getList(logReceivers, getLocale(), MentionableAtomicAsset::getAsMention), true)
                        .addField(getString("state0_maction"), getString("state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true);

            case 1:
                setOptions(new String[] { getString("empty") });
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[] { getString("empty") });
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[] { getString("empty") });
                return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setOptions(getString("state0_mactionlist").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));

            default:
                return null;
        }
    }

}
