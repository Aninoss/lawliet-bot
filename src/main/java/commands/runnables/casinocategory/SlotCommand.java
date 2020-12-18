package commands.runnables.casinocategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import commands.runnables.CasinoAbstract;
import constants.Category;
import constants.Emojis;
import constants.LogStatus;
import constants.Permission;
import core.EmbedFactory;
import core.TextManager;
import core.schedule.MainScheduler;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;


@CommandProperties(
        trigger = "slot",
        emoji = "🎰",
        executableWithoutArgs = true,
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        aliases = {"slots", "slotmachine"}
)
public class SlotCommand extends CasinoAbstract implements OnReactionAddListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(SlotCommand.class);

    private String log;
    private int winLevel;
    private int progress;
    private LogStatus logStatus;
    private boolean first;
    private final String[] FRUITS_CONTAINER = {"🍇", "🍈", "🍉", "🍊", "🍋", "🍌", "🍍", "🍎", "🍐", "🍑", "🍒", "🍓", "🆒"};
    private final double[] WIN_POSSABILITIES = {10, 20, 100, 200};
    private final double[] WIN_AMOUNT_ADJUSTMENT = {1.6, 1.2, 0.8, 0.4};
    private int[] fruits;
    private final String ALL_EMOJI = "✅";

    public SlotCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {
            try {
                useCalculatedMultiplicator = false;
                first = true;
                progress = 0;
                fruits = new int[3];

                double n = new Random().nextDouble();

                winLevel = 0;
                for (int i = 0; i < WIN_POSSABILITIES.length; i++) {
                    n -= 1.0 / WIN_POSSABILITIES[i];
                    if (n <= 0) {
                        winLevel = i + 1;
                        break;
                    }
                }

                setFruits();

                message = event.getChannel().sendMessage(getEmbed()).get();
                message.addReaction(ALL_EMOJI).get();

                return true;
            } catch (Throwable e) {
                handleError(e, event.getServerTextChannel().get());
                return false;
            }
        }
        return false;
    }

    private void setFruits() {
        switch (winLevel) {
            case 0:
                setFruitsLevel0();
                break;

            case 1:
            case 2:
                setFruitsLevel1_2();
                break;

            case 3:
            case 4:
                setFruitsLevel3_4();
                break;

            default:
                setFruitsLevel0();
        }
    }

    private void setFruitsLevel3_4() {
        Random r = new Random();

        int selectedFruit = r.nextInt(FRUITS_CONTAINER.length - 1);
        if (winLevel == 4) selectedFruit =FRUITS_CONTAINER.length - 1;
        for (int i = 0; i < 3; i++) {
            fruits[i] = selectedFruit;
        }
    }

    private void setFruitsLevel1_2() {
        Random r = new Random();

        int n;
        int notSelected = r.nextInt(3);
        int selectedFruit = r.nextInt(FRUITS_CONTAINER.length - 1);
        if (winLevel == 2) selectedFruit =FRUITS_CONTAINER.length - 1;
        for (int i = 0; i < 3; i++) {
            if (notSelected != i) {
                fruits[i] = selectedFruit;
            } else {
                do {
                    n = r.nextInt(FRUITS_CONTAINER.length);
                } while (n == selectedFruit);
                fruits[i] = n;
            }
        }
    }

    private void setFruitsLevel0() {
        Random r = new Random();

        int n;
        for (int i = 0; i < 3; i++) {
            do {
                n = r.nextInt(FRUITS_CONTAINER.length);
            } while (n == fruits[0] || n == fruits[1] || n == fruits[2]);
            fruits[i] = n;
        }
    }

    private EmbedBuilder getEmbed() {
        String key = "template";
        if (first) {
            key = "template_start";
            first = false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key,
                player.getDisplayName(server),
                StringUtil.numToString(coinsInput),
                getSpinningWheel(0),
                getSpinningWheel(1),
                getSpinningWheel(2),
                Emojis.SLOT_DR,
                Emojis.SLOT_LR,
                Emojis.SLOT_DLR,
                Emojis.SLOT_DL,
                Emojis.SLOT_UD,
                Emojis.SLOT_UR,
                Emojis.SLOT_LR1,
                Emojis.SLOT_ULR,
                Emojis.SLOT_LR2,
                Emojis.SLOT_LR3,
                Emojis.SLOT_UL,
                ALL_EMOJI,
                Emojis.SPACEHOLDER
                ));

        if (coinsInput != 0) EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));

        eb = EmbedUtil.addLog(eb, logStatus, log);
        if (!active) eb = addRetryOption(eb);

        return eb;
    }

    private String getSpinningWheel(int i) {
        if (progress <= i) return Emojis.SLOT_SPINNING;
        else return FRUITS_CONTAINER[fruits[i]];
    }

    private void manageEnd() throws ExecutionException {
        if (progress < 3) return;

        removeReactionListener(getReactionMessage());
        log = getString("end", winLevel);
        if (winLevel == 0) {
            logStatus = LogStatus.LOSE;
            onLose();
        }
        else {
            logStatus = LogStatus.WIN;

            winMultiplicator = WIN_POSSABILITIES[winLevel-1]/WIN_POSSABILITIES.length*WIN_AMOUNT_ADJUSTMENT[winLevel-1] -1;
            onWin();
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!active) {
            onReactionAddRetry(event);
            return;
        }

        if (event.getEmoji().isUnicodeEmoji()) {
            if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(ALL_EMOJI)) {
                removeReactionListener();
                message.edit(getEmbed()).exceptionally(ExceptionLogger.get());

                MainScheduler.getInstance().schedule(1000, "slot_0", () -> unlockFruit(0));
                MainScheduler.getInstance().schedule(2000, "slot_1", () -> unlockFruit(1));
                MainScheduler.getInstance().schedule(3000, "slot_2", () -> unlockFruit(2));
                MainScheduler.getInstance().schedule(4000, "slot_results", () -> {
                    try {
                        manageEnd();
                        message.getCurrentCachedInstance().ifPresent(m -> m.edit(getEmbed()).exceptionally(ExceptionLogger.get()));
                    } catch (ExecutionException e) {
                        LOGGER.error("Slot exception", e);
                    }
                });
            }
        }
    }

    private void unlockFruit(int i) {
        progress = i + 1;
        message.getCurrentCachedInstance().ifPresent(m -> m.edit(getEmbed()).exceptionally(ExceptionLogger.get()));
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {
        if (active) {
            progress = 3;
            manageEnd();
            message.edit(getEmbed());
        }
    }
}
