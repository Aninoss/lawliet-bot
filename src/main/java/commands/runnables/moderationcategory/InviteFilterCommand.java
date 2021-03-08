package commands.runnables.moderationcategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.LogStatus;
import constants.Response;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.spblock.DBSPBlock;
import mysql.modules.spblock.SPBlockBean;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "invitefilter",
        botPermissions = PermissionDeprecated.MANAGE_MESSAGES,
        userPermissions = PermissionDeprecated.MANAGE_MESSAGES | PermissionDeprecated.KICK_MEMBERS | PermissionDeprecated.BAN_MEMBERS,
        emoji = "✉️",
        executableWithoutArgs = true,
        aliases = { "invitesfilter", "spblock", "inviteblock", "spfilter", "invitesblock" }
)
public class InviteFilterCommand extends Command implements OnNavigationListenerOld {
    
    private SPBlockBean spBlockBean;
    private CustomObservableList<User> ignoredUsers, logReceivers;
    private CustomObservableList<ServerTextChannel> ignoredChannels;

    public InviteFilterCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String args) throws Throwable {
        spBlockBean = DBSPBlock.getInstance().retrieve(event.getGuild().getIdLong());
        ignoredUsers = spBlockBean.getIgnoredUserIds().transform(userId -> event.getServer().get().getMemberById(userId), DiscordEntity::getId);
        logReceivers = spBlockBean.getLogReceiverUserIds().transform(userId -> event.getServer().get().getMemberById(userId), DiscordEntity::getId);
        ignoredChannels = spBlockBean.getIgnoredChannelIds().transform(userId -> event.getServer().get().getTextChannelById(userId), DiscordEntity::getId);
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
                ArrayList<ServerTextChannel> channelIgnoredList = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
                if (channelIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), inputString));
                    return Response.FALSE;
                } else {
                    ignoredChannels.clear();
                    ignoredChannels.addAll(channelIgnoredList);
                    setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
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
                    spBlockBean.setAction(SPBlockBean.ActionList.values()[i]);
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
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                       .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getLocale(), spBlockBean.isActive()), true)
                       .addField(getString("state0_mignoredusers"), new ListGen<User>().getList(ignoredUsers, getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_mignoredchannels"), new ListGen<ServerTextChannel>().getList(ignoredChannels, getLocale(), Mentionable::getMentionTag), true)
                       .addField(getString("state0_mlogreciever"), new ListGen<User>().getList(logReceivers, getLocale(), User::getMentionTag), true)
                       .addField(getString("state0_maction"),getString("state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true);

            case 1:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[]{getString("empty")});
                return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setOptions(getString("state0_mactionlist").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));

            default:
                return null;
        }
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {
    }

    @Override
    public int getMaxReactionNumber() {
        return 5;
    }

}
