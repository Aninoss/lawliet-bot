package commands.runnables.fisherycategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ComponentMenuAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.TextManager;
import core.utils.StringUtil;
import modules.fishery.FisheryPowerUp;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberStatsData;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.section.SectionContentComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "fisherystats",
        emoji = "📈",
        executableWithoutArgs = true,
        usesExtEmotes = true
)
public class FisheryStatsCommand extends ComponentMenuAbstract {

    private boolean resetLock = true;

    public FisheryStatsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerListeners(event.getMember());
        return true;
    }

    @Draw(state = STATE_ROOT)
    public List<ContainerChildComponent> drawRoot(Member member) {
        setDescription(getString("description"));
        ArrayList<ContainerChildComponent> components = new ArrayList<>();

        FisheryMemberStatsData stats = new FisheryGuildData(member.getGuild().getIdLong()).getMemberData(member.getIdLong()).getStats();
        boolean enabled = stats.getEnabled();

        Button enabledButton = buttonSecondary(TextManager.getString(getLocale(), TextManager.GENERAL, "onoff", enabled), Emojis.SWITCHES_DOT[enabled ? 1 : 0], e -> {
            resetLock = true;
            stats.setEnabled(!enabled);
            return true;
        });
        ArrayList<SectionContentComponent> enabledSectionComponents = new ArrayList<>();
        enabledSectionComponents.add(TextDisplay.of(getString("enabled")));
        if (!enabled) {
            enabledSectionComponents.add(TextDisplay.of(getString("notenabled")));
        }
        Section enabledSection = Section.of(enabledButton, enabledSectionComponents);
        components.add(enabledSection);

        String treasureChests = getString("treasure_chests",
                StringUtil.numToString(stats.getTreasureChestsOpened()),
                StringUtil.numToString((int) (stats.getTreasureChestsSuccessful() * 100.0 / stats.getTreasureChestsOpened())),
                StringUtil.numToString(stats.getTreasureChestsTotalCoinsReceived())
        );
        components.add(Separator.createInvisible(Separator.Spacing.SMALL));
        components.add(TextDisplay.of(treasureChests));

        StringBuilder powerUps = new StringBuilder(getString("powerups"));
        for (int i = 0; i < FisheryPowerUp.values().length; i++) {
            FisheryPowerUp powerUp = FisheryPowerUp.values()[i];
            powerUps.append("\n- ")
                    .append(TextManager.getString(getGuildEntity(), getLocale(), Category.FISHERY_SETTINGS.getId(), "fishery_powerup", i))
                    .append(": **")
                    .append(StringUtil.numToString(stats.getPowerUpReceived(powerUp)))
                    .append("**");
        }
        components.add(Separator.createInvisible(Separator.Spacing.SMALL));
        components.add(TextDisplay.of(powerUps.toString()));

        Button resetButton = buttonDanger(getString("reset"), Emojis.MENU_RESET, e -> {
            if (resetLock) {
                resetLock = false;
                setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
            } else {
                stats.reset();
            }
            return true;
        });
        components.add(Separator.createInvisible(Separator.Spacing.SMALL));
        components.add(ActionRow.of(resetButton));

        return components;
    }

}
