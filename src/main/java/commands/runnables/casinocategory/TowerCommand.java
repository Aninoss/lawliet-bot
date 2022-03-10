package commands.runnables.casinocategory;

import java.util.Locale;
import java.util.Random;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

@CommandProperties(
        trigger = "tower",
        emoji = "🏗️️",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "crash" }
)
public class TowerCommand extends CasinoAbstract {

    private final double MULTIPLIER_STEP = 0.5;

    private boolean showMoreText = false;
    private boolean crashed = false;
    private boolean falling = false;
    private int towerLevel = 0;
    private double towerMultiplier = 1.0;
    private final Random r = new Random();

    public TowerCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);
    }

    @Override
    public boolean onGameStart(CommandEvent event, String args) {
        showMoreText = true;
        return true;
    }

    @Override
    public boolean onButtonCasino(ButtonClickEvent event) throws Throwable {
        int i = Integer.parseInt(event.getComponentId());
        if (i == 0) {
            if (towerMultiplier < 10.0) {
                onRaise(event.getMember());
            } else {
                setLog(LogStatus.FAILURE, getString("cap"));
                showMoreText = true;
            }
        } else {
            onSell(event.getMember());
        }
        return true;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        final int LEVEL_LIMIT = 12;

        final String GRASS_EMOJI = Emojis.TOWER_GRAS;
        final String EMPTY_EMOJI = Emojis.FULL_SPACE_EMOTE;

        String[] towerEmojis;
        if (crashed) {
            towerEmojis = Emojis.TOWER_BASE_BROKEN;
        } else {
            towerEmojis = Emojis.TOWER_BASE;
        }

        String[] towerEmojisAnimated = Emojis.TOWER_BASE_FALLING;

        /* build tower */
        StringBuilder towerText = new StringBuilder(towerLevel < LEVEL_LIMIT ? (Emojis.ZERO_WIDTH_SPACE + "\n") : "");
        for (int i = 0; i < Math.min(LEVEL_LIMIT, towerLevel) && !crashed; i++) {
            if (i == 0 && falling && towerLevel <= LEVEL_LIMIT) {
                towerText.append(getString("base", EMPTY_EMOJI, towerEmojisAnimated[0], towerEmojisAnimated[1]))
                        .append("\n");
            } else {
                towerText.append(getString("base", EMPTY_EMOJI, towerEmojis[0], towerEmojis[1]))
                        .append("\n");
            }
        }
        towerText.append(getString("template", EMPTY_EMOJI, towerEmojis[0], towerEmojis[1], GRASS_EMOJI));

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, towerText.toString());
        eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("template_start", showMoreText,
                playerName,
                StringUtil.numToString(coinsInput),
                StringUtil.doubleToString(towerMultiplier, 2, getLocale())
        ), false);

        if (getStatus() == Status.ACTIVE) {
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, "0", getString("raise", StringUtil.doubleToString(MULTIPLIER_STEP, 2, getLocale()))),
                    Button.of(ButtonStyle.SECONDARY, "1", getString("sell"))
            );
        }

        showMoreText = false;
        crashed = false;
        falling = false;
        return eb;
    }

    private void onRaise(Member member) {
        if (r.nextDouble() <= (towerMultiplier / (towerMultiplier + MULTIPLIER_STEP))) {
            towerLevel++;
            towerMultiplier += MULTIPLIER_STEP;
            showMoreText = true;
            falling = true;
        } else {
            lose(member);
            setLog(LogStatus.LOSE, getString("lost"));
            towerLevel = 0;
            crashed = true;
        }
    }

    private void onSell(Member member) {
        if (towerMultiplier > 1) {
            win(member, towerMultiplier - 1);
        } else {
            endGame(member);
        }
        setLog(LogStatus.WIN, getString("win", StringUtil.doubleToString(towerMultiplier, 2, getLocale())));
    }

}
