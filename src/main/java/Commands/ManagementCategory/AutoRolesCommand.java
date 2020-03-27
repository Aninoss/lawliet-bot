package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.NavigationHelper;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import General.*;
import General.Mention.MentionFinder;
import MySQL.DBServerOld;
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
        aliases = {"basicroles", "autorole", "aroles"}
)
public class AutoRolesCommand extends Command implements onNavigationListener {

    private static final int MAX_ROLES = 12;

    private ArrayList<Role> roles;
    private static ArrayList<Long> busyServers = new ArrayList<>();
    private NavigationHelper<Role> roleNavigationHelper;

    public AutoRolesCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws SQLException, IOException {
        if (firstTime) {
            roles = DBServerOld.getBasicRolesFromServer(event.getServer().get());
            roleNavigationHelper = new NavigationHelper<>(this, roles, Role.class, MAX_ROLES);
            checkRolesWithLog(roles, event.getMessage().getUserAuthor().get());
            return Response.TRUE;
        }

        if (state == 1) {
            ArrayList<Role> roleList = MentionFinder.getRoles(event.getMessage(), inputString).getList();
            return roleNavigationHelper.addData(roleList, inputString, event.getMessage().getUserAuthor().get(), 0, role -> {
                try {
                    DBServerOld.addBasicRoles(event.getServer().get(), role);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
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
                        roleNavigationHelper.startDataAdd(1);
                        return true;

                    case 1:
                        roleNavigationHelper.startDataRemove(2);
                        return true;

                    case 2:
                        if (!busyServers.contains(event.getServer().get().getId())) {
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
                        if (!busyServers.contains(event.getServer().get().getId())) {
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
                return roleNavigationHelper.removeData(i, 0, role -> {
                    try {
                        DBServerOld.removeBasicRoles(event.getServer().get(), role);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
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
                return roleNavigationHelper.drawDataAdd();

            case 2:
                return roleNavigationHelper.drawDataRemove();
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    private void transferRoles(Server server, ArrayList<Role> roles) {
        Thread t = new Thread(() -> {
            busyServers.add(server.getId());

            for (User user : server.getMembers()) {
                for (Role role : roles) {
                    if (!role.getUsers().contains(user)) role.addUser(user);
                }
            }

            busyServers.remove(server.getId());
        });
        t.setName("autoroles_transfering");
        t.start();
    }

    private void removeRoles(Server server, ArrayList<Role> roles) {
        Thread t = new Thread(() -> {
            busyServers.add(server.getId());

            for (User user : server.getMembers()) {
                for (Role role : roles) {
                    if (role.getUsers().contains(user)) role.removeUser(user);
                }
            }

            busyServers.remove(server.getId());
        });
        t.setName("autoroles_transfering");
        t.start();
    }
}
