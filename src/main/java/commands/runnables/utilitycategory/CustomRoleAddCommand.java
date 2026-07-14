package commands.runnables.utilitycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.ComponentsUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.CustomRoles;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "customroleadd",
        userGuildPermissions = Permission.MANAGE_ROLES,
        botGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "\uD83D\uDD16",
        executableWithoutArgs = false,
        requiresFullMemberCache = true,
        aliases = { "customroleassign", "customrolecreate" }
)
public class CustomRoleAddCommand extends Command {

    public CustomRoleAddCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        List<Member> targetMembers = MentionUtil.getMembers(event.getGuild(), args, event.getRepliedMember()).getList();
        Member targetMember = targetMembers.isEmpty() ? null : targetMembers.get(0);
        if (targetMember == null) {
            TextDisplay content = TextDisplay.of(TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions"));
            drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, content))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        CustomRoles.cleanUp(getGuildEntity(), event.getGuild());
        Map<Long, Long> customRoles = getGuildEntity().getCustomRoles();
        if (customRoles.containsKey(targetMember.getIdLong())) {
            TextDisplay content = TextDisplay.of(getString("already_assigned"));
            drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, content))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }
        if (event.getGuild().getRoles().size() >= 250) {
            TextDisplay content = TextDisplay.of(getString("too_many_roles"));
            drawMessageNew(ComponentsUtil.createCommandComponentTreeError(this, content))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        String roleName = getString("new_role_name", targetMember.getEffectiveName());
        Role role = event.getGuild().createRole()
                .setName(StringUtil.shortenString(roleName, 100))
                .setPermissions()
                .reason(getCommandLanguage().getTitle())
                .complete();
        event.getGuild().addRoleToMember(targetMember, role)
                .reason(getCommandLanguage().getTitle())
                .complete();

        GuildEntity guildEntity = getGuildEntity();
        guildEntity.beginTransaction();
        guildEntity.getCustomRoles().put(targetMember.getIdLong(), role.getIdLong());
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.ADD_CUSTOM_ROLE, event.getMember(), null,  null, List.of(targetMember.getIdLong()));
        guildEntity.commitTransaction();

        TextDisplay content = TextDisplay.of(getString("success", role.getName(), targetMember.getEffectiveName()));
        drawMessageNew(ComponentsUtil.createCommandComponentTree(this, content))
                .exceptionally(ExceptionLogger.get());
        return true;
    }

}
