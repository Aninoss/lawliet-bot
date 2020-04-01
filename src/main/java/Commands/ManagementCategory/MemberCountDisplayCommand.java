package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import General.*;
import General.Mention.MentionTools;
import MySQL.MemberCountDisplays.DBMemberCountDisplays;
import MySQL.MemberCountDisplays.MemberCountBean;
import MySQL.MemberCountDisplays.MemberCountDisplay;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.channel.ServerVoiceChannelUpdater;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.*;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandProperties(
        trigger = "mcdisplays",
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83E\uDDEE️",
        thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/trends-icon.png",
        executable = true,
        aliases = {"membercountdisplays", "memberscountdisplays", "memberdisplays", "mdisplays", "countdisplays", "displays", "mcdisplay" }
)
public class MemberCountDisplayCommand extends Command implements onNavigationListener  {

    private MemberCountBean memberCountBean;
    private ServerVoiceChannel currentVC = null;
    private String currentName = null;
    
    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            memberCountBean = DBMemberCountDisplays.getInstance().getBean(event.getServer().get().getId());
            return Response.TRUE;
        }

        if (state == 1) {
            ArrayList<ServerVoiceChannel> vcList = MentionTools.getVoiceChannels(event.getMessage(), inputString).getList();
            if (vcList.size() == 0) {
                String checkString = inputString.toLowerCase();
                if (checkString.contains("%members") || checkString.contains("%users") || checkString.contains("%bots")) {
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
                if (!checkManageChannelWithLog(channel)) return Response.FALSE;

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
                        deleteNavigationMessage();
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
                }
                return false;

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

                if (i == 0 && currentName != null && currentVC != null) {
                    try {
                        ServerVoiceChannelUpdater updater = currentVC.createUpdater();
                        for (Role role : currentVC.getOverwrittenRolePermissions().keySet()) {
                            PermissionsBuilder permissions = currentVC.getOverwrittenPermissions().get(role).toBuilder();
                            permissions.setState(PermissionType.CONNECT, PermissionState.DENIED);
                            updater.addPermissionOverwrite(role, permissions.build());
                        }
                        for (User user : currentVC.getOverwrittenUserPermissions().keySet()) {
                            PermissionsBuilder permissions = currentVC.getOverwrittenPermissions().get(user).toBuilder();
                            permissions.setState(PermissionType.CONNECT, PermissionState.DENIED);
                            updater.addPermissionOverwrite(user, permissions.build());
                        }
                        renameVC(event.getServer().get(), getLocale(), updater, currentName);
                        updater.update().get();
                    } catch (ExecutionException e) {
                        //e.printStackTrace();
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
        }
        return false;
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
                                    try {
                                        if (bean.getVoiceChannel().isPresent()) {
                                            return getString("state0_displays", bean.getVoiceChannel().get().getName(), bean.getMask());
                                        } else {
                                            return getString("state0_displays", "???", bean.getMask());
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    return "";
                                })), false);

            case 1:
                if (currentName != null && currentVC != null) setOptions(new String[]{getString("state1_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description", Optional.ofNullable(currentVC).map(Nameable::getName).orElse(notSet), highlightVariables(Optional.ofNullable(currentName).orElse(notSet))), getString("state1_title"));

            case 2:
                ArrayList<MemberCountDisplay> channelNames = new ArrayList<>(memberCountBean.getMemberCountBeanSlots().values());
                String[] roleStrings = new String[channelNames.size()];
                for(int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = channelNames.get(i).getMask();
                }
                setOptions(roleStrings);
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) {}

    @Override
    public int getMaxReactionNumber() {
        return 5;
    }

    private String highlightVariables(String str) {
        return replaceVariables(str, "`%MEMBERS`", "`%USERS`", "`%BOTS`");
    }

    public static void manage(Locale locale, Server server) throws SQLException, ExecutionException, InterruptedException {
        ArrayList<MemberCountDisplay> displays = new ArrayList<>(DBMemberCountDisplays.getInstance().getBean(server.getId()).getMemberCountBeanSlots().values());
        for(MemberCountDisplay display: displays) {
            if (display.getVoiceChannel().isPresent()) {
                ServerVoiceChannel voiceChannel = display.getVoiceChannel().get();
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, "mcdisplays", voiceChannel, Permission.MANAGE_CHANNEL)) {
                    ServerVoiceChannelUpdater updater = voiceChannel.createUpdater();
                    renameVC(server, locale, updater, display.getMask());
                    updater.update();
                }
            }
        }
    }

    public static void renameVC(Server server, Locale locale, ServerVoiceChannelUpdater updater, String name) {
        long members = server.getMembers().size();
        long botMembers = server.getMembers().stream().filter(User::isBot).count();

        updater.setName(replaceVariables(name,
                StringTools.numToString(locale, members),
                StringTools.numToString(locale, members - botMembers),
                StringTools.numToString(locale, botMembers)
        ));
    }

    public static String replaceVariables(String string, String arg1, String arg2, String arg3) {
        return string.replaceAll("(?i)" + Pattern.quote("%members"), Matcher.quoteReplacement(arg1))
                .replaceAll("(?i)" + Pattern.quote("%users"), Matcher.quoteReplacement(arg2))
                .replaceAll("(?i)" + Pattern.quote("%bots"), Matcher.quoteReplacement(arg3));
    }

}
