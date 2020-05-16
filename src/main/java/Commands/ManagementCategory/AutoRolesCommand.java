package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnNavigationListener;
import CommandSupporters.Command;
import CommandSupporters.NavigationHelper;
import Constants.Permission;
import Constants.Response;
import Core.*;
import Core.Mention.MentionUtil;
import MySQL.Modules.AutoRoles.AutoRolesBean;
import MySQL.Modules.AutoRoles.DBAutoRoles;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
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
        executable = true,
        aliases = {"basicroles", "autorole", "aroles", "joinroles", "jroles"}
)
public class AutoRolesCommand extends Command implements OnNavigationListener {

    private static final int MAX_ROLES = 12;

    private AutoRolesBean autoRolesBean;
    private NavigationHelper<Role> roleNavigationHelper;
    private CustomObservableList<Role> roles;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        autoRolesBean = DBAutoRoles.getInstance().getBean(event.getServer().get().getId());
        roles = autoRolesBean.getRoleIds().transform(roleId -> autoRolesBean.getServer().get().getRoleById(roleId), DiscordEntity::getId);
        roleNavigationHelper = new NavigationHelper<>(this, roles, Role.class, MAX_ROLES);
        checkRolesWithLog(roles, event.getMessage().getUserAuthor().get());
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws IOException, ExecutionException {
        if (state == 1) {
            List<Role> roleList = MentionUtil.getRoles(event.getMessage(), inputString).getList();
            return roleNavigationHelper.addData(roleList, inputString, event.getMessage().getUserAuthor().get(), 0);
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
                return roleNavigationHelper.removeData(i, 0);
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

}
