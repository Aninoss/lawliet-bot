package commands.runnables.casinocategory;

import java.util.Locale;
import java.util.Random;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "tower",
        emoji = "🏗️️",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        aliases = { "crash" }
)
public class TowerCommand extends CasinoAbstract {

    private final String[] ACTION_EMOJIS = { "🛠️", "💰" };
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
    public String[] onGameStart(GuildMessageReceivedEvent event, String args) {
        showMoreText = true;

        return ACTION_EMOJIS;
    }

    @Override
    public boolean onReactionCasino(GenericGuildMessageReactionEvent event) {
        for (int i = 0; i < 2; i++) {
            if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), ACTION_EMOJIS[i])) {
                if (i == 0) {
                    if (towerMultiplier < 10.0) {
                        onRaise();
                    } else {
                        setLog(LogStatus.FAILURE, getString("cap"));
                        showMoreText = true;
                    }
                } else {
                    onSell();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        final int LEVEL_LIMIT = 12;

        final String GRASS_EMOJI = Emojis.TOWER_GRAS;
        final String EMPTY_EMOJI = Emojis.SPACEHOLDER;

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
                StringUtil.doubleToString(towerMultiplier, 2, getLocale()),
                ACTION_EMOJIS[0],
                StringUtil.doubleToString(MULTIPLIER_STEP, 2, getLocale()),
                ACTION_EMOJIS[1]
        ), false);

        showMoreText = false;
        crashed = false;
        falling = false;
        return eb;
    }

    private void onRaise() {
        if (r.nextDouble() <= (towerMultiplier / (towerMultiplier + MULTIPLIER_STEP))) {
            towerLevel++;
            towerMultiplier += MULTIPLIER_STEP;
            showMoreText = true;
            falling = true;
        } else {
            lose();
            setLog(LogStatus.LOSE, getString("lost"));
            towerLevel = 0;
            crashed = true;
        }
    }

    private void onSell() {
        if (towerMultiplier > 1) {
            win(towerMultiplier - 1);
        } else {
            endGame();
        }
        setLog(LogStatus.WIN, getString("win", StringUtil.doubleToString(towerMultiplier, 2, getLocale())));
    }

}
