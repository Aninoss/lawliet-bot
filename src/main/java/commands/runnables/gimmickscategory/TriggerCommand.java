package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import core.EmbedFactory;
import modules.graphics.TriggerGraphics;

import java.util.Locale;

@CommandProperties(
        trigger = "trigger",
        botPermissions = PermissionDeprecated.ATTACH_FILES,
        withLoadingBar = true,
        emoji = "\uD83D\uDCA2",
        executableWithoutArgs = true,
        aliases = {"triggered"}
)
public class TriggerCommand extends MemberAccountAbstract {

    public TriggerCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String args) throws Throwable {
        return EmbedFactory.getEmbedDefault(this,getString("template", user.getDisplayName(server)))
                .setImage(TriggerGraphics.createImageTriggered(user), "gif");
    }

}
