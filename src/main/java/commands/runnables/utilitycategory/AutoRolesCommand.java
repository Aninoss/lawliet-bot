package commands.runnables.utilitycategory;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.Response;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicRole;
import core.atomicassets.MentionableAtomicAsset;
import core.utils.MentionUtil;
import mysql.modules.autoroles.AutoRolesBean;
import mysql.modules.autoroles.DBAutoRoles;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "autoroles",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "👪",
        executableWithoutArgs = true,
        aliases = { "basicroles", "autorole", "aroles", "joinroles", "jroles" }
)
public class AutoRolesCommand extends NavigationAbstract {

    private static final int MAX_ROLES = 12;

    private NavigationHelper<AtomicRole> roleNavigationHelper;
    private CustomObservableList<AtomicRole> roles;

    public AutoRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        AutoRolesBean autoRolesBean = DBAutoRoles.getInstance().retrieve(event.getGuild().getIdLong());
        roles = AtomicRole.transformIdList(event.getGuild(), autoRolesBean.getRoleIds());
        roleNavigationHelper = new NavigationHelper<>(this, roles, AtomicRole.class, MAX_ROLES);
        checkRolesWithLog(event.getGuild(), event.getMember(), roles.stream().map(r -> r.get().orElse(null)).collect(Collectors.toList()));
        registerNavigationListener(12);
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<Role> roleList = MentionUtil.getRoles(event.getMessage(), input).getList();
            return roleNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMember(), 0);
        }

        return null;
    }

    @Override
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) {
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
    public EmbedBuilder draw(int state) {
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mroles"), new ListGen<AtomicRole>().getList(roles, getLocale(), MentionableAtomicAsset::getAsMention), true);

            case 1:
                return roleNavigationHelper.drawDataAdd();

            case 2:
                return roleNavigationHelper.drawDataRemove();

            default:
                return null;
        }
    }

}
