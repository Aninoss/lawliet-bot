package Commands.FisherySettingsCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import Core.*;
import Core.Mention.MentionUtil;
import Core.Utils.StringUtil;
import Modules.Fishery;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "fisheryroles",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "📜",
        executable = true,
        aliases = {"fishingroles", "fishroles", "fisheryr"}
)
public class FisheryRolesCommand extends Command implements OnNavigationListener {

    private static final int MAX_ROLES = 50;
    private final static Logger LOGGER = LoggerFactory.getLogger(FisheryRolesCommand.class);

    private ServerBean serverBean;
    private FisheryServerBean fisheryServerBean;
    private CustomObservableList<Role> roles;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
        fisheryServerBean = DBFishery.getInstance().getBean(event.getServer().get().getId());
        roles = fisheryServerBean.getRoles();

        checkRolesWithLog(roles, null);
        return true;
    }
    
    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            case 1:
                ArrayList<Role> roleList = MentionUtil.getRoles(event.getMessage(), inputString).getList();
                if (roleList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    if (!checkRolesWithLog(roleList, event.getMessage().getUserAuthor().get())) return Response.FALSE;

                    int existingRoles = 0;
                    for (Role role : roleList) {
                        if (roles.contains(role)) existingRoles++;
                    }

                    if (existingRoles >= roleList.size()) {
                        setLog(LogStatus.FAILURE, getString("roleexists", roleList.size() != 1));
                        return Response.FALSE;
                    }

                    int rolesAdded = 0;
                    for (Role role : roleList) {
                        if (!roles.contains(role)) {
                            roles.add(role);
                            rolesAdded++;
                            roles.sort(Comparator.comparingInt(Role::getPosition));
                        }
                    }

                    setLog(LogStatus.SUCCESS, getString("roleadd", (rolesAdded - existingRoles) != 1, String.valueOf(rolesAdded)));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
                ArrayList<ServerTextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    ServerTextChannel channel = channelList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        serverBean.setFisheryAnnouncementChannelId(channel.getId());
                        setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 4:
                if (inputString.contains("-") && !inputString.replaceFirst("-", "").contains("-")) {
                    String[] parts = (inputString + " ").split("-");
                    long priceMin = StringUtil.filterLongFromString(parts[0]);
                    long priceMax = StringUtil.filterLongFromString(parts[1]);

                    if (priceMin >= -1 && priceMax >= -1 && priceMin <= Settings.MAX && priceMax <= Settings.MAX) {
                        if (priceMin == -1) priceMin = serverBean.getFisheryRoleMin();
                        if (priceMax == -1) priceMax = serverBean.getFisheryRoleMax();
                        serverBean.setFisheryRolePrices(priceMin, priceMax);
                        setLog(LogStatus.SUCCESS, getString("pricesset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number2", "0", StringUtil.numToString(getLocale(), Settings.MAX)));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, getString("prices_wrongvalues"));
                    return Response.FALSE;
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
                        if (roles.size() < MAX_ROLES) {
                            setState(1);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanyroles", String.valueOf(MAX_ROLES)));
                            return true;
                        }

                    case 1:
                        if (roles.size() > 0) {
                            setState(2);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("norolesset"));
                            return true;
                        }

                    case 2:
                        serverBean.toggleFisherySingleRoles();
                        setLog(LogStatus.SUCCESS, getString("singleroleset", serverBean.isFisherySingleRoles()));
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
                }
                return false;

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < roles.size()) {
                    roles.remove(i);
                    setLog(LogStatus.SUCCESS, getString("roleremove"));
                    if (roles.size() == 0) setState(0);
                    return true;
                }
                return false;

            case 3:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i == 0) {
                    serverBean.setFisheryAnnouncementChannelId(null);
                    setState(0);
                    setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                    return true;
                }
                return false;

            case 4:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                return false;

            default:
                return false;
        }
    }

    private String getRoleString(Role role) {
        int n = roles.indexOf(role);
        try {
            return getString("state0_rolestring", role.getMentionTag(), StringUtil.numToString(Fishery.getFisheryRolePrice(role.getServer(), new ArrayList<>(fisheryServerBean.getRoleIds()), n)));
        } catch (ExecutionException e) {
            LOGGER.error("Exception", e);
            return "";
        }
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));

                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description", String.valueOf(MAX_ROLES)))
                        .addField(getString("state0_mroles"), new ListGen<Role>().getList(roles, getLocale(), this::getRoleString), false)
                        .addField(getString("state0_msinglerole", StringUtil.getOnOffForBoolean(getLocale(), serverBean.isFisherySingleRoles())), getString("state0_msinglerole_desc"), false)
                        .addField(getString("state0_mannouncementchannel"), serverBean.getFisheryAnnouncementChannel().map(Mentionable::getMentionTag).orElse(notSet), false)
                        .addField(getString("state0_mroleprices"), getString("state0_mroleprices_desc", StringUtil.numToString(getLocale(), serverBean.getFisheryRoleMin()), StringUtil.numToString(getLocale(), serverBean.getFisheryRoleMax())), false);

            case 1:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                String[] roleStrings = new String[roles.size()];
                for(int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = getRoleString(roles.get(i));
                }
                setOptions(roleStrings);

                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));
                return eb;

            case 3:
                setOptions(new String[]{getString("state3_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title"));

            case 4:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"), getString("state4_title"));

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

}
