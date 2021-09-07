package commands.runnables.casinocategory;

import java.util.Locale;
import java.util.Random;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import commands.Category;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.schedule.MainScheduler;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "slot",
        emoji = "🎰",
        executableWithoutArgs = true,
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        usesExtEmotes = true,
        aliases = { "slots", "slotmachine" }
)
public class SlotCommand extends CasinoAbstract {

    private final String[] FRUITS_CONTAINER = { "🍇", "🍈", "🍉", "🍊", "🍋", "🍌", "🍍", "🍎", "🍐", "🍑", "🍒", "🍓", "🆒" };
    private final double[] WIN_POSSIBILITIES = { 10, 20, 100, 200 };
    private final double[] WIN_AMOUNT_ADJUSTMENT = { 1.4, 1.15, 0.85, 0.6 };

    private int winLevel;
    private int progress = 0;
    private boolean first = true;
    private final int[] fruits = new int[3];

    public SlotCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);
    }

    @Override
    public boolean onGameStart(GuildMessageReceivedEvent event, String args) {
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
        setComponents(
                Button.of(ButtonStyle.PRIMARY, "go", getString("go")),
                BUTTON_CANCEL
        );
        return true;
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
    public boolean onButtonCasino(ButtonClickEvent event) throws Throwable {
        deregisterListenersWithButtons();

        Member member = event.getMember();
        MainScheduler.schedule(1000, "slot_0", () -> unlockFruit(member, 0));
        MainScheduler.schedule(2000, "slot_1", () -> unlockFruit(member, 1));
        MainScheduler.schedule(3000, "slot_2", () -> unlockFruit(member, 2));
        MainScheduler.schedule(4000, "slot_results", () -> manageEnd(member));

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
                Emojis.FULL_SPACE_EMOTE
        ));

        if (coinsInput != 0) {
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));
        }

        return eb;
    }

    private void unlockFruit(Member member, int i) {
        progress = i + 1;
        drawMessage(draw(member));
    }

    private String getSpinningWheel(int i) {
        if (progress <= i) {
            return Emojis.SLOT_SPINNING;
        } else {
            return FRUITS_CONTAINER[fruits[i]];
        }
    }

    private void manageEnd(Member member) {
        if (progress < 3) return;

        deregisterListeners();
        if (winLevel == 0) {
            lose(member);
            setLog(LogStatus.LOSE, getString("end", 0));
        } else {
            win(member, WIN_POSSIBILITIES[winLevel - 1] / WIN_POSSIBILITIES.length * WIN_AMOUNT_ADJUSTMENT[winLevel - 1] - 1);
            setLog(LogStatus.WIN, getString("end", winLevel));
        }
        drawMessage(draw(member));
    }

}
