package commands.runnables.configurationcategory;

import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.Locale;

@CommandProperties(
        trigger = "triggerdelete",
        userGuildPermissions = { Permission.MANAGE_SERVER, Permission.MESSAGE_MANAGE },
        botGuildPermissions = { Permission.MESSAGE_MANAGE },
        emoji = "\uD83D\uDDD1",
        executableWithoutArgs = true,
        patreonRequired = true,
        usesExtEmotes = true,
        aliases = { "triggerremove", "starterremove", "startermessagedelete", "startermessageremove", "messagedelete", "messageremove", "starterdelete" }
)
public class TriggerDeleteCommand extends CommandOnOffSwitchAbstract {

    public TriggerDeleteCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

    @Override
    protected boolean isActive(Member member) {
        return getGuildEntity().getRemoveAuthorMessageEffectively();
    }

    @Override
    protected boolean setActive(Member member, boolean active) {
        getGuildEntity().beginTransaction();
        getGuildEntity().setRemoveAuthorMessage(active);
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.REMOVE_AUTHOR_MESSAGE, member, null, active);
        getGuildEntity().commitTransaction();
        return true;
    }

}
