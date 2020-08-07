package Commands.CasinoCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnReactionAddListener;
import Commands.CasinoAbstract;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Settings;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.Utils.StringUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;


@CommandProperties(
        trigger = "tower",
        emoji = "🏗️️",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        executable = true,
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
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {
            useCalculatedMultiplicator = false;

            message = event.getChannel().sendMessage(getEmbed(true, false, false)).get();
            for (String str : EMOJIS) message.addReaction(str);

            return true;
        }
        return false;
    }

    private EmbedBuilder getEmbed(boolean showMoreText, boolean crashed, boolean falling) {
        final int LEVEL_LIMIT = 12;

        final String GRASS_EMOJI = DiscordApiCollection.getInstance().getHomeEmojiById(734843199985811556L).getMentionTag();
        final String EMPTY_EMOJI = DiscordApiCollection.getInstance().getHomeEmojiById(417016019622559755L).getMentionTag();
        String[] towerEmojis = new String[2];
        if (crashed) {
            towerEmojis[0] = DiscordApiCollection.getInstance().getHomeEmojiById(734842201254920223L).getMentionTag();
            towerEmojis[1] = DiscordApiCollection.getInstance().getHomeEmojiById(734842200755535912L).getMentionTag();
        } else {
            towerEmojis[0] = DiscordApiCollection.getInstance().getHomeEmojiById(734836799003688981L).getMentionTag();
            towerEmojis[1] = DiscordApiCollection.getInstance().getHomeEmojiById(734836799402409986L).getMentionTag();
        }
        String[] towerEmojisAnimated = {
                DiscordApiCollection.getInstance().getHomeEmojiById(735259827563135030L).getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiById(735259827382779985L).getMentionTag()
        };

        /* build tower */
        StringBuilder towerText = new StringBuilder(towerLevel < LEVEL_LIMIT ? (Settings.EMPTY_EMOJI + "\n") : "");
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

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, towerText.toString());
        eb.addField(Settings.EMPTY_EMOJI, getString("template_start", showMoreText,
                player.getDisplayName(server),
                StringUtil.numToString(getLocale(), coinsInput),
                StringUtil.doubleToString(towerMultiplier, 2, getLocale()),
                EMOJIS[0],
                StringUtil.doubleToString(MULTIPLIER_STEP, 2, getLocale()),
                EMOJIS[1]
        ));

        eb = EmbedFactory.addLog(eb, logStatus, log);
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
            onLose();
            towerLevel = 0;
            logStatus = LogStatus.LOSE;
            log = getString("lost");
            message.edit(getEmbed(false, true, false)).exceptionally(ExceptionLogger.get());
        }
    }

    private void onSell() throws ExecutionException {
        if (towerMultiplier > 1) {
            winMultiplicator = towerMultiplier - 1;
            onWin();
        } else {
            onGameEnd();
        }
        logStatus = LogStatus.WIN;
        log = getString("win", StringUtil.doubleToString(towerMultiplier, 2, getLocale()));
        message.edit(getEmbed(false, false, false)).exceptionally(ExceptionLogger.get());
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
