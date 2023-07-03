package commands.runnables.utilitycategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

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
        getGuildEntity().commitTransaction();
        return true;
    }

}
