package commands.runnables.utilitycategory;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicRole;
import core.atomicassets.MentionableAtomicAsset;
import core.utils.MentionUtil;
import mysql.modules.stickyroles.DBStickyRoles;
import mysql.modules.stickyroles.StickyRolesData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "stickyroles",
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "📍",
        executableWithoutArgs = true,
        aliases = { "keeproles", "stickyrole", "keeprole" }
)
public class StickyRolesCommand extends NavigationAbstract {

    private static final int MAX_ROLES = 12;

    private NavigationHelper<AtomicRole> roleNavigationHelper;
    private CustomObservableList<AtomicRole> roles;

    public StickyRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        StickyRolesData stickyRolesData = DBStickyRoles.getInstance().retrieve(event.getGuild().getIdLong());
        roles = AtomicRole.transformIdList(event.getGuild(), stickyRolesData.getRoleIds());
        roleNavigationHelper = new NavigationHelper<>(this, roles, AtomicRole.class, MAX_ROLES);
        checkRolesWithLog(event.getMember(), roles.stream().map(r -> r.get().orElse(null)).collect(Collectors.toList()));
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state == 1) {
            List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
            return roleNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMember(), 0);
        }

        return null;
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
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
    public EmbedBuilder draw(Member member, int state) {
        switch (state) {
            case 0:
                setComponents(getString("state0_options").split("\n"));
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
