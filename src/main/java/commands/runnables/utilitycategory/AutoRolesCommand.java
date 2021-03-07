package commands.runnables.utilitycategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListenerOld;
import commands.Command;
import commands.NavigationHelper;
import constants.Response;
import core.*;
import core.utils.MentionUtil;
import mysql.modules.autoroles.AutoRolesBean;
import mysql.modules.autoroles.DBAutoRoles;






import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "autoroles",
        botPermissions = PermissionDeprecated.MANAGE_ROLES,
        userPermissions = PermissionDeprecated.MANAGE_ROLES,
        emoji = "👪",
        executableWithoutArgs = true,
        aliases = {"basicroles", "autorole", "aroles", "joinroles", "jroles"}
)
public class AutoRolesCommand extends Command implements OnNavigationListenerOld {

    private static final int MAX_ROLES = 12;

    private AutoRolesBean autoRolesBean;
    private NavigationHelper<Role> roleNavigationHelper;
    private CustomObservableList<Role> roles;

    public AutoRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        autoRolesBean = DBAutoRoles.getInstance().retrieve(event.getServer().get().getId());
        roles = autoRolesBean.getRoleIds().transform(roleId -> autoRolesBean.getGuild().get().getRoleById(roleId), DiscordEntity::getId);
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
                return roleNavigationHelper.removeData(i, 0);

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
                       .addField(getString("state0_mroles"), new ListGen<Role>().getList(roles, getLocale(), Role::getMentionTag), true);

            case 1:
                return roleNavigationHelper.drawDataAdd();

            case 2:
                return roleNavigationHelper.drawDataRemove();

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
