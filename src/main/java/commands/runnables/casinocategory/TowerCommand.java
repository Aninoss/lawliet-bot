package commands.runnables.casinocategory;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "tower",
        emoji = "🏗️️",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        executableWithoutArgs = true,
        aliases = { "crash" }
)
public class TowerCommand extends CasinoAbstract implements OnReactionAddListener {

    private final String[] EMOJIS = {"🛠️", "💰"};
    private final double MULTIPLIER_STEP = 0.5;

    private int towerLevel = 0;
    private double towerMultiplier = 1.0;
    private String log;
    private LogStatus logStatus;
    private final Random r = new Random();

    public TowerCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        if (onGameStart(event, followedString)) {
            try {
                useCalculatedMultiplicator = false;

                message = event.getChannel().sendMessage(getEmbed(true, false, false)).get();
                for (String str : EMOJIS) message.addReaction(str).get();

                return true;
            } catch (Throwable e) {
                handleError(e, event.getServerTextChannel().get());
                return false;
            }
        }
        return false;
    }

    private EmbedBuilder getEmbed(boolean showMoreText, boolean crashed, boolean falling) {
        final int LEVEL_LIMIT = 12;

        final String GRASS_EMOJI = Emojis.TOWER_GRAS;
        final String EMPTY_EMOJI = Emojis.SPACEHOLDER;

        String[] towerEmojis;
        if (crashed)
            towerEmojis = Emojis.TOWER_BASE_BROKEN;
        else
            towerEmojis = Emojis.TOWER_BASE;

        String[] towerEmojisAnimated = Emojis.TOWER_BASE_FALLING;

        /* build tower */
        StringBuilder towerText = new StringBuilder(towerLevel < LEVEL_LIMIT ? (Emojis.EMPTY_EMOJI + "\n") : "");
        for(int i = 0; i < Math.min(LEVEL_LIMIT, towerLevel) && !crashed; i++) {
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
        eb.addField(Emojis.EMPTY_EMOJI, getString("template_start", showMoreText,
                player.getDisplayName(server),
                StringUtil.numToString(coinsInput),
                StringUtil.doubleToString(towerMultiplier, 2, getLocale()),
                EMOJIS[0],
                StringUtil.doubleToString(MULTIPLIER_STEP, 2, getLocale()),
                EMOJIS[1]
        ));

        eb = EmbedUtil.addLog(eb, logStatus, log);
        if (!active)
            eb = addRetryOption(eb);

        return eb;
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!active) {
            onReactionAddRetry(event);
            return;
        }

        if (event.getEmoji().isUnicodeEmoji()) {
            for(int i = 0; i < 2; i++) {
                if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(EMOJIS[i])) {
                    if (i == 0) {
                        if (towerMultiplier < 10.0) {
                            onRaise();
                        } else {
                            logStatus = LogStatus.FAILURE;
                            log = getString("cap");
                            message.edit(getEmbed(true, false, false)).exceptionally(ExceptionLogger.get());
                        }
                    } else {
                        onSell();
                    }
                    return;
                }
            }
        }
    }

    private void onRaise() throws ExecutionException, InterruptedException {
        if (r.nextDouble() <= (towerMultiplier / (towerMultiplier + MULTIPLIER_STEP))) {
            towerLevel++;
            towerMultiplier += MULTIPLIER_STEP;
            message.edit(getEmbed(true, false, true)).get();
        } else {
            lose();
            towerLevel = 0;
            logStatus = LogStatus.LOSE;
            log = getString("lost");
            message.edit(getEmbed(false, true, false)).exceptionally(ExceptionLogger.get());
        }
    }

    private void onSell() throws ExecutionException {
        if (towerMultiplier > 1) {
            winMultiplicator = towerMultiplier - 1;
            win();
        } else {
            endGame();
        }
        logStatus = LogStatus.WIN;
        log = getString("win", StringUtil.doubleToString(towerMultiplier, 2, getLocale()));
        message.getCurrentCachedInstance().ifPresent(m -> m.edit(getEmbed(false, false, false)).exceptionally(ExceptionLogger.get()));
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {
        if (active) {
            onSell();
        }
    }
}
