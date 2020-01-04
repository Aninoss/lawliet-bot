package Commands.Management;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import General.*;
import General.Mention.MentionFinder;
import MySQL.DBServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

@CommandProperties(
        trigger = "autoroles",
        botPermissions = Permission.MANAGE_ROLES_ON_SERVER,
        userPermissions = Permission.MANAGE_ROLES_ON_SERVER,
        emoji = "\uD83D\uDC6A",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/User-group-icon.png",
        executable = true,
        aliases = {"basicroles", "autorole"}
)
public class AutoRolesCommand extends Command implements onNavigationListener {
    
    private ArrayList<Role> roles;
    private static ArrayList<Server> busyServers = new ArrayList<>();

    public AutoRolesCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws SQLException, IOException {
        if (firstTime) {
            roles = DBServer.getBasicRolesFromServer(event.getServer().get());
            checkRolesWithLog(roles);
            return Response.TRUE;
        }

        if (state == 1) {
            ArrayList<Role> roleList = MentionFinder.getRoles(event.getMessage(), inputString).getList();
            if (roleList.size() == 0) {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                return Response.FALSE;
            } else {
                if (!checkRolesWithLog(roleList)) return Response.FALSE;

                int existingRoles = 0;
                for(Role role: roleList) {
                    if (roles.contains(role)) existingRoles ++;
                }

                if (existingRoles >= roleList.size()) {
                    setLog(LogStatus.FAILURE, getString("roleexists", roleList.size() != 1));
                    return Response.FALSE;
                }

                int n = 0;
                for(Role role: roleList) {
                    if (!roles.contains(role)) {
                        if (roles.size() < getMaxReactionNumber()) {
                            roles.add(role);
                            DBServer.addBasicRoles(event.getServer().get(), role);
                            n++;
                        }
                    }
                }

                setLog(LogStatus.SUCCESS, getString("roleadd", n != 1));
                setState(0);
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
                        if (roles.size() < getMaxReactionNumber()) {
                            setState(1);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanyroles", String.valueOf(getMaxReactionNumber())));
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
                        if (!busyServers.contains(event.getServer().get())) {
                            if (roles.size() > 0) {
                                transferRoles(event.getServer().get(), roles);
                                setLog(LogStatus.SUCCESS, getString("transferset", roles.size() != 1));
                                return true;
                            } else {
                                setLog(LogStatus.FAILURE, getString("norolesset"));
                                return true;
                            }
                        } else {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "role_busy"));
                            return true;
                        }

                    case 3:
                        if (!busyServers.contains(event.getServer().get())) {
                            if (roles.size() > 0) {
                                removeRoles(event.getServer().get(), roles);
                                setLog(LogStatus.SUCCESS, getString("removeset", roles.size() != 1));
                                return true;
                            } else {
                                setLog(LogStatus.FAILURE, getString("norolesset"));
                                return true;
                            }
                        } else {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "role_busy"));
                            return true;
                        }
                }
                return false;

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < roles.size() && i >= 0) {
                    DBServer.removeBasicRoles(event.getServer().get(), roles.remove(i));
                    setLog(LogStatus.SUCCESS, getString("roleremove"));
                    setState(0);
                    return true;
                }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                       .addField(getString("state0_mroles"), new ListGen<Role>().getList(roles, getLocale(), Role::getMentionTag), true);

            case 1:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                String[] roleStrings = new String[roles.size()];
                for(int i=0; i<roleStrings.length; i++) {
                    roleStrings[i] = roles.get(i).getMentionTag();
                }
                setOptions(roleStrings);
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 8;
    }

    private void transferRoles(Server server, ArrayList<Role> roles) {
        Thread t = new Thread(() -> {
            busyServers.add(server);

            for (User user : server.getMembers()) {
                for (Role role : roles) {
                    if (!role.getUsers().contains(user)) role.addUser(user);
                }
            }

            busyServers.remove(server);
        });
        t.setName("autoroles_transfering");
        t.start();
    }

    private void removeRoles(Server server, ArrayList<Role> roles) {
        Thread t = new Thread(() -> {
            busyServers.add(server);

            for (User user : server.getMembers()) {
                for (Role role : roles) {
                    if (role.getUsers().contains(user)) role.removeUser(user);
                }
            }

            busyServers.remove(server);
        });
        t.setName("autoroles_transfering");
        t.start();
    }
}
