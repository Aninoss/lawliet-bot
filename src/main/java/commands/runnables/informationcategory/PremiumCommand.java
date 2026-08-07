package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.ExceptionLogger;
import core.patreon.PatreonCache;
import core.utils.ComponentsUtil;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "premium",
        emoji = "\uD83D\uDCB3",
        executableWithoutArgs = true,
        aliases = { "donate", "donation", "patreon" }
)
public class PremiumCommand extends Command {

    public PremiumCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        components.add(TextDisplay.of(getString("info")));
        components.add(Separator.createInvisible(Separator.Spacing.SMALL));

        boolean subscriptionBoolean = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), false);
        components.add(TextDisplay.of(getString("subscription", subscriptionBoolean)));

        int unlockState;
        if (PatreonCache.getInstance().isUnlockedPlus(event.getGuild().getIdLong())) {
            unlockState = 2;
        } else if (PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())) {
            unlockState = 1;
        } else {
            unlockState = 0;
        }
        components.add(TextDisplay.of(getString("server_unlocked", unlockState)));
        components.add(Separator.createInvisible(Separator.Spacing.SMALL));
        components.add(ActionRow.of(ComponentsUtil.getPatreonButton(getLocale())));

        drawMessageNew(ComponentsUtil.createCommandComponentTree(this, components))
                .exceptionally(ExceptionLogger.get());
        return true;
    }

}
