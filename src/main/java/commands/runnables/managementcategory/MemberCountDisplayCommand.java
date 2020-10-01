package commands.runnables.managementcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import commands.Command;
import constants.LogStatus;
import constants.Permission;
import constants.Response;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.ListGen;
import core.utils.MentionUtil;
import core.TextManager;
import core.utils.PermissionUtil;
import core.utils.StringUtil;
import mysql.modules.membercountdisplays.DBMemberCountDisplays;
import mysql.modules.membercountdisplays.MemberCountBean;
import mysql.modules.membercountdisplays.MemberCountDisplay;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelUpdater;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.*;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@CommandProperties(
        trigger = "mcdisplays",
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "️🧮️",
        executable = true,
        aliases = {"membercountdisplays", "memberscountdisplays", "memberdisplays", "mdisplays", "countdisplays", "displays", "mcdisplay" }
)
public class MemberCountDisplayCommand extends Command implements OnNavigationListener {

    private MemberCountBean memberCountBean;
    private ServerVoiceChannel currentVC = null;
    private String currentName = null;

    public MemberCountDisplayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        memberCountBean = DBMemberCountDisplays.getInstance().getBean(event.getServer().get().getId());
        memberCountBean.getMemberCountBeanSlots().trim(vcId -> event.getServer().get().getVoiceChannelById(vcId));
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state == 1) {
            ArrayList<ServerVoiceChannel> vcList = MentionUtil.getVoiceChannels(event.getMessage(), inputString).getList();
            if (vcList.size() == 0) {
                String checkString = inputString.toLowerCase();
                if (!modules.MemberCountDisplay.replaceVariables(checkString, "", "", "", "").equals(checkString)) {
                    if (inputString.length() <= 50) {
                        currentName = inputString;
                        setLog(LogStatus.SUCCESS, getString("nameset"));
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("nametoolarge", "50"));
                        return Response.FALSE;
                    }
                }

                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;
            } else {
                ServerVoiceChannel channel = vcList.get(0);

                ArrayList<Integer> missingPermissions = PermissionUtil.getMissingPermissionListForUser(channel.getServer(), channel, DiscordApiCollection.getInstance().getYourself(), Permission.MANAGE_CHANNEL | Permission.MANAGE_CHANNEL_PERMISSIONS | Permission.CONNECT);
                if (missingPermissions.size() > 0) {
                    String permissionsList = new ListGen<Integer>().getList(missingPermissions, ListGen.SLOT_TYPE_BULLET, n -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, String.valueOf(n)));
                    setLog(LogStatus.FAILURE, getString("missing_perms", permissionsList));
                    return Response.FALSE;
                }

                if (memberCountBean.getMemberCountBeanSlots().containsKey(channel.getId())) {
                    setLog(LogStatus.FAILURE, getString("alreadyexists"));
                    return Response.FALSE;
                }

                currentVC = channel;

                setLog(LogStatus.SUCCESS, getString("vcset"));
                return Response.TRUE;
            }
        }

        return null;
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
                        if (memberCountBean.getMemberCountBeanSlots().size() < 5) {
                            setState(1);
                            currentVC = null;
                            currentName = null;
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanydisplays"));
                            return true;
                        }

                    case 1:
                        if (memberCountBean.getMemberCountBeanSlots().size() > 0) {
                            setState(2);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("nothingtoremove"));
                            return true;
                        }

                    default:
                        return false;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

                if (i == 0 && currentName != null && currentVC != null) {
                    try {
                        ServerVoiceChannelUpdater updater = currentVC.createUpdater();
                        /*for (Long roleId : currentVC.getOverwrittenRolePermissions().keySet()) {
                            PermissionsBuilder permissions = currentVC.getOverwrittenPermissions().get(roleId).toBuilder();
                            permissions.setState(PermissionType.CONNECT, PermissionState.DENIED);
                            updater.addPermissionOverwrite(event.getServer().get().getRoleById(roleId).get(), permissions.build());
                        }
                        for (Long userId : currentVC.getOverwrittenUserPermissions().keySet()) {
                            PermissionsBuilder permissions = currentVC.getOverwrittenPermissions().get(userId).toBuilder();
                            permissions.setState(PermissionType.CONNECT, PermissionState.DENIED);
                            updater.addPermissionOverwrite(event.getServer().get().getMemberById(userId).get(), permissions.build());
                        }*/
                        for (Role role : currentVC.getOverwrittenRolePermissions().keySet()) {
                            PermissionsBuilder permissions = currentVC.getOverwrittenPermissions().get(role).toBuilder();
                            permissions.setState(PermissionType.CONNECT, PermissionState.DENIED);
                            updater.addPermissionOverwrite(event.getServer().get().getRoleById(role.getId()).get(), permissions.build());
                        }
                        for (User user : currentVC.getOverwrittenUserPermissions().keySet()) {
                            PermissionsBuilder permissions = currentVC.getOverwrittenPermissions().get(user).toBuilder();
                            permissions.setState(PermissionType.CONNECT, PermissionState.DENIED);
                            updater.addPermissionOverwrite(event.getServer().get().getMemberById(user.getId()).get(), permissions.build());
                        }

                        Role everyoneRole = event.getServer().get().getEveryoneRole();
                        PermissionsBuilder permissions = currentVC.getOverwrittenPermissions(everyoneRole).toBuilder();
                        permissions.setState(PermissionType.CONNECT, PermissionState.DENIED);
                        updater.addPermissionOverwrite(everyoneRole, permissions.build());

                        User yourself = DiscordApiCollection.getInstance().getYourself();
                        Permissions ownPermissions = currentVC.getOverwrittenPermissions(yourself)
                                .toBuilder()
                                .setState(PermissionType.MANAGE_CHANNELS, PermissionState.ALLOWED)
                                .setState(PermissionType.MANAGE_ROLES, PermissionState.ALLOWED)
                                .setState(PermissionType.CONNECT, PermissionState.ALLOWED)
                                .build();
                        updater.addPermissionOverwrite(yourself, ownPermissions);

                        String newVCName = modules.MemberCountDisplay.getNewVCName(event.getServer().get(), getLocale(), currentName);
                        updater.setName(newVCName)
                                .update().get(10, TimeUnit.SECONDS);
                    } catch (ExecutionException | TimeoutException e) {
                        //Ignore
                        setLog(LogStatus.FAILURE, getString("nopermissions"));
                        return true;
                    }

                    memberCountBean.getMemberCountBeanSlots().put(currentVC.getId(), new MemberCountDisplay(event.getServer().get().getId(), currentVC.getId(), currentName));

                    setLog(LogStatus.SUCCESS, getString("displayadd"));
                    setState(0);

                    return true;
                }
                return false;

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < memberCountBean.getMemberCountBeanSlots().size()) {
                    memberCountBean.getMemberCountBeanSlots().remove(new ArrayList<>(memberCountBean.getMemberCountBeanSlots().keySet()).get(i));

                    setLog(LogStatus.SUCCESS, getString("displayremove"));
                    setState(0);
                    return true;
                }

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                        .addField(getString("state0_mdisplays"), highlightVariables(new ListGen<MemberCountDisplay>()
                                .getList(memberCountBean.getMemberCountBeanSlots().values(), getLocale(), bean -> {
                                    if (bean.getVoiceChannel().isPresent()) {
                                        return getString("state0_displays", StringUtil.escapeMarkdown(bean.getVoiceChannel().get().getName()), StringUtil.escapeMarkdown(bean.getMask()));
                                    } else {
                                        return getString("state0_displays", "???", StringUtil.escapeMarkdown(bean.getMask()));
                                    }
                                })), false);

            case 1:
                if (currentName != null && currentVC != null) setOptions(new String[]{getString("state1_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description", StringUtil.escapeMarkdown(Optional.ofNullable(currentVC).map(Nameable::getName).orElse(notSet)), highlightVariables(StringUtil.escapeMarkdown(Optional.ofNullable(currentName).orElse(notSet)))), getString("state1_title"));

            case 2:
                ArrayList<MemberCountDisplay> channelNames = new ArrayList<>(memberCountBean.getMemberCountBeanSlots().values());
                String[] roleStrings = new String[channelNames.size()];
                for(int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = channelNames.get(i).getMask();
                }
                setOptions(roleStrings);
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            default:
                return null;
        }
    }

    @Override
    public void onNavigationTimeOut(Message message) {}

    @Override
    public int getMaxReactionNumber() {
        return 5;
    }

    private String highlightVariables(String str) {
        return modules.MemberCountDisplay.replaceVariables(str, "`%MEMBERS`", "`%USERS`", "`%BOTS`", "`%BOOSTS`");
    }

}
