package commands.runnables.casinocategory;

import java.util.Locale;
import java.util.Random;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Category;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.schedule.MainScheduler;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "slot",
        emoji = "🎰",
        executableWithoutArgs = true,
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        aliases = { "slots", "slotmachine" }
)
public class SlotCommand extends CasinoAbstract {

    private final String[] FRUITS_CONTAINER = { "🍇", "🍈", "🍉", "🍊", "🍋", "🍌", "🍍", "🍎", "🍐", "🍑", "🍒", "🍓", "🆒" };
    private final double[] WIN_POSSIBILITIES = { 10, 20, 100, 200 };
    private final double[] WIN_AMOUNT_ADJUSTMENT = { 1.3, 1.1, 0.9, 0.7 };
    private final String ALL_EMOJI = "✅";

    private int winLevel;
    private int progress = 0;
    private boolean first = true;
    private final int[] fruits = new int[3];

    public SlotCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);
    }

    @Override
    public String[] onGameStart(GuildMessageReceivedEvent event, String args) throws Throwable {
        double n = new Random().nextDouble();

        winLevel = 0;
        for (int i = 0; i < WIN_POSSIBILITIES.length; i++) {
            n -= 1.0 / WIN_POSSIBILITIES[i];
            if (n <= 0) {
                winLevel = i + 1;
                break;
            }
        }

        setFruits();
        return new String[]{ ALL_EMOJI };
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
        if (winLevel == 4) selectedFruit = FRUITS_CONTAINER.length - 1;
        for (int i = 0; i < 3; i++) {
            fruits[i] = selectedFruit;
        }
    }

    private void setFruitsLevel1_2() {
        Random r = new Random();

        int n;
        int notSelected = r.nextInt(3);
        int selectedFruit = r.nextInt(FRUITS_CONTAINER.length - 1);
        if (winLevel == 2) selectedFruit = FRUITS_CONTAINER.length - 1;
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

    @Override
    public boolean onReactionCasino(GenericGuildMessageReactionEvent event) {
        removeReactionListener();

        MainScheduler.getInstance().schedule(1000, "slot_0", () -> unlockFruit(0));
        MainScheduler.getInstance().schedule(2000, "slot_1", () -> unlockFruit(1));
        MainScheduler.getInstance().schedule(3000, "slot_2", () -> unlockFruit(2));
        MainScheduler.getInstance().schedule(4000, "slot_results", this::manageEnd);

        return true;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        String key = "template";
        if (first) {
            key = "template_start";
            first = false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(
                key,
                playerName,
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

        if (coinsInput != 0)
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));

        return eb;
    }

    private void unlockFruit(int i) {
        progress = i + 1;
        drawMessage(draw());
    }

    private String getSpinningWheel(int i) {
        if (progress <= i) return Emojis.SLOT_SPINNING;
        else return FRUITS_CONTAINER[fruits[i]];
    }

    private void manageEnd() {
        if (progress < 3) return;

        removeReactionListener();
        if (winLevel == 0) {
            lose();
            setLog(LogStatus.LOSE, getString("end", 0));
        } else {
            win(WIN_POSSIBILITIES[winLevel - 1] / WIN_POSSIBILITIES.length * WIN_AMOUNT_ADJUSTMENT[winLevel - 1] - 1);
            setLog(LogStatus.WIN, getString("end", winLevel));
        }
        drawMessage(draw());
    }

}
