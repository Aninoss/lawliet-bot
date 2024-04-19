package commands.runnables.nsfwinteractionscategory;

import commands.listeners.CommandProperties;
import commands.runnables.interactionscategory.CustomRolePlaySfwCommand;

import java.util.Locale;

@CommandProperties(
        trigger = "customrp_sfw",
        emoji = "🧩",
        executableWithoutArgs = false,
        nsfw = true,
        requiresFullMemberCache = true
)
public class CustomRolePlayNsfwCommand extends CustomRolePlaySfwCommand {

    public CustomRolePlayNsfwCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

}
