package Commands.Management;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import General.*;
import General.Mention.MentionFinder;
import MySQL.DBServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Permissionable;
import org.javacord.api.entity.channel.ServerTextChannel;
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
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandProperties(
        trigger = "mcdisplays",
        botPermissions = 0,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83E\uDDEE️",
        thumbnail = "http://icons.iconarchive.com/icons/elegantthemes/beautiful-flat/128/trends-icon.png",
        executable = true,
        aliases = {"membercountdisplays", "memberscountdisplays", "memberdisplays", "mdisplays", "countdisplays", "displays"}
)
public class MemberCountDisplayCommand extends Command implements onNavigationListener  {

    public MemberCountDisplayCommand() {
        super();
    }

    private ArrayList<Pair<ServerVoiceChannel, String>> displays;
    private ServerVoiceChannel currentVC = null;
    private String currentName = null;
    
    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            displays = DBServer.getMemberCountDisplays(event.getServer().get());
            return Response.TRUE;
        }

        if (state == 1) {
            ArrayList<ServerVoiceChannel> vcList = MentionFinder.getVoiceChannels(event.getMessage(), inputString).getList();
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

                for(Pair<ServerVoiceChannel, String> display: displays) {
                    if (display.getKey().getId() == channel.getId()) {
                        setLog(LogStatus.FAILURE, getString("alreadyexists"));
                        return Response.FALSE;
                    }
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
                        if (displays.size() < 5) {
                            setState(1);
                            currentVC = null;
                            currentName = null;
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanydisplays"));
                            return true;
                        }

                    case 1:
                        if (displays.size() > 0) {
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

                    Pair<ServerVoiceChannel, String> dispay = new Pair<>(currentVC, currentName);
                    DBServer.addMemberCountDisplay(dispay);

                    setLog(LogStatus.SUCCESS, getString("displayadd"));
                    setState(0);

                    return true;
                }
                return false;

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < displays.size()) {
                    DBServer.removeMemberCountDisplay(displays.get(i));
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
                        .addField(getString("state0_mdisplays"), highlightVariables(new ListGen<Pair<ServerVoiceChannel, String>>()
                                .getList(displays, getLocale(), pair -> {
                                    try {
                                        return getString("state0_displays", pair.getKey().getName(), pair.getValue());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    return "";
                                })), false);

            case 1:
                if (currentName != null && currentVC != null) setOptions(new String[]{getString("state1_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description", Tools.getStringIfNotNull(currentVC, notSet), highlightVariables(Tools.getStringIfNotNull(currentName, notSet))), getString("state1_title"));

            case 2:
                String[] roleStrings = new String[displays.size()];
                for(int i=0; i<roleStrings.length; i++) {
                    roleStrings[i] = displays.get(i).getKey().getName();
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
        ArrayList<Pair<ServerVoiceChannel, String>> displays = DBServer.getMemberCountDisplays(server);
        for(Pair<ServerVoiceChannel, String> display: displays) {
            if (PermissionCheckRuntime.getInstance().botHasPermission(locale, "mcdisplays", display.getKey(), Permission.MANAGE_CHANNEL)) {
                ServerVoiceChannelUpdater updater = display.getKey().createUpdater();
                renameVC(server, locale, updater, display.getValue());
                updater.update().get();
            }
        }
    }

    public static void renameVC(Server server, Locale locale, ServerVoiceChannelUpdater updater, String name) {
        updater.setName(replaceVariables(name,
                Tools.numToString(locale, server.getMemberCount()),
                Tools.numToString(locale, server.getMembers().stream().filter(user -> !user.isBot()).count()),
                Tools.numToString(locale, server.getMembers().stream().filter(User::isBot).count())
        ));
    }

    public static String replaceVariables(String string, String arg1, String arg2, String arg3) {
        return string.replaceAll("(?i)" + Pattern.quote("%members"), Matcher.quoteReplacement(arg1))
                .replaceAll("(?i)" + Pattern.quote("%users"), Matcher.quoteReplacement(arg2))
                .replaceAll("(?i)" + Pattern.quote("%bots"), Matcher.quoteReplacement(arg3));
    }

}
