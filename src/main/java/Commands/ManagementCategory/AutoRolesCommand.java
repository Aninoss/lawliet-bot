package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.NavigationHelper;
import Constants.Permission;
import Constants.Response;
import General.*;
import General.Mention.MentionTools;
import MySQL.Modules.AutoRoles.AutoRolesBean;
import MySQL.Modules.AutoRoles.DBAutoRoles;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "autoroles",
        botPermissions = Permission.MANAGE_ROLES,
        userPermissions = Permission.MANAGE_ROLES,
        emoji = "\uD83D\uDC6A",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/User-group-icon.png",
        executable = true,
        aliases = {"basicroles", "autorole", "aroles"}
)
public class AutoRolesCommand extends Command implements onNavigationListener {

    private static final int MAX_ROLES = 12;

    private AutoRolesBean autoRolesBean;
    private NavigationHelper<Role> roleNavigationHelper;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        autoRolesBean = DBAutoRoles.getInstance().getBean(event.getServer().get().getId());
        roleNavigationHelper = new NavigationHelper<>(this, autoRolesBean.getRoleIds().transform(roleId -> event.getServer().get().getRoleById(roleId)), Role.class, MAX_ROLES);
        checkRolesWithLog(autoRolesBean.getRoleIds().transform(roleId -> autoRolesBean.getServer().get().getRoleById(roleId)), event.getMessage().getUserAuthor().get());
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws IOException, ExecutionException {
        if (state == 1) {
            List<Role> roleList = MentionTools.getRoles(event.getMessage(), inputString).getList();
            return roleNavigationHelper.addData(roleList, inputString, event.getMessage().getUserAuthor().get(), 0, role -> autoRolesBean.getRoleIds().add(role.getId()));
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
                }
                return false;

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            case 2:
                return roleNavigationHelper.removeData(i, 0, role -> autoRolesBean.getRoleIds().remove(role.getId()));
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                       .addField(getString("state0_mroles"), new ListGen<Role>().getList(autoRolesBean.getRoleIds().transform(roleId -> autoRolesBean.getServer().get().getRoleById(roleId)), getLocale(), Role::getMentionTag), true);

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

}
