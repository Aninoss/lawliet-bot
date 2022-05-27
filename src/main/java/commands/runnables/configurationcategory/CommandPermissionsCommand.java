package commands.runnables.configurationcategory;

import java.util.Locale;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import constants.LogStatus;
import core.EmbedFactory;
import core.CommandPermissions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandProperties(
        trigger = "cperms",
        userGuildPermissions = Permission.MANAGE_PERMISSIONS,
        emoji = "⛔",
        executableWithoutArgs = true,
        aliases = { "commandpermissions", "cpermissions", "commandp" }
)
public class CommandPermissionsCommand extends Command implements OnButtonListener {

    private boolean synched = false;

    public CommandPermissionsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerButtonListener(event.getMember());
        return true;
    }

    @Nullable
    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        if (!synched) {
            setComponents(getString("button"));
        }
        return EmbedFactory.getEmbedDefault(this, getString("message0") + "\n\n" + getString("message1"));
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        if (CommandPermissions.transferCommandPermissions(event.getGuild())) {
            setLog(LogStatus.SUCCESS, getString("success"));
        } else {
            setLog(LogStatus.FAILURE, getString("failed"));
        }
        synched = true;
        deregisterListeners();
        return true;
    }

}
