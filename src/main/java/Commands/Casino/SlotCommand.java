package Commands.Casino;

import CommandListeners.CommandProperties;
import CommandListeners.onReactionAddListener;
import CommandListeners.onRecievedListener;
import Constants.LogStatus;
import General.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;


@CommandProperties(
        trigger = "slot",
        emoji = "\uD83C\uDFB0",
        thumbnail = "http://icons.iconarchive.com/icons/flat-icons.com/flat/128/Coins-icon.png",
        executable = true,
        deleteOnTimeOut = false,
        aliases = {"slots", "slotmachine"}
)
public class SlotCommand extends Casino implements onRecievedListener, onReactionAddListener {

    private String log;
    private int winLevel;
    private boolean[] progress;
    private LogStatus logStatus;
    private boolean first;
    private final String[] FRUITS_CONTAINER = {"\uD83C\uDF47", "\uD83C\uDF48", "\uD83C\uDF49", "\uD83C\uDF4A", "\uD83C\uDF4B", "\uD83C\uDF4C", "\uD83C\uDF4D", "\uD83C\uDF4E", "\uD83C\uDF50", "\uD83C\uDF51", "\uD83C\uDF52", "\uD83C\uDF53", "\uD83C\uDD92"};
    private final String[] NUMBERS = {"1⃣", "2⃣", "3⃣"};
    private final double[] WIN_POSSABILITIES = {10, 20, 100, 200};
    private final double[] WIN_AMOUNT_ADJUSTMENT = {1.6, 1.2, 0.8, 0.4};
    private int[] fruits;
    private final String ALL_EMOJI = "✅";

    public SlotCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {
            useCalculatedMultiplicator = false;
            first = true;
            progress = new boolean[3];
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
            message.addReaction(ALL_EMOJI);
            for (String str : NUMBERS) message.addReaction(str);

            return true;
        }
        return false;
    }

    private void setFruits() {
        Random r = new Random();

        if (winLevel == 0) {
            int n;
            for (int i = 0; i < 3; i++) {
                do {
                    n = r.nextInt(FRUITS_CONTAINER.length);
                } while (n == fruits[0] || n == fruits[1] || n == fruits[2]);
                fruits[i] = n;
            }
        }

        if (winLevel == 1 || winLevel == 2) {
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

        if (winLevel == 3 || winLevel == 4) {
            int selectedFruit = r.nextInt(FRUITS_CONTAINER.length - 1);
            if (winLevel == 4) selectedFruit =FRUITS_CONTAINER.length - 1;
            for (int i = 0; i < 3; i++) {
                fruits[i] = selectedFruit;
            }
        }
    }

    private EmbedBuilder getEmbed() throws IOException {
        String key = "template";
        if (first) {
            key = "template_start";
            first = false;
        }

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString(key,
                player.getDisplayName(server),
                Tools.numToString(getLocale(), coinsInput),
                getSpinningWheel(0),
                getSpinningWheel(1),
                getSpinningWheel(2),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotdr").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotlr").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotdlr").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotdl").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotud").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotur").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotlr1").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotulr").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotlr2").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotlr3").getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiByName("slotul").getMentionTag(),
                ALL_EMOJI
                ));

        if (coinsInput != 0) eb.setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "casino_footer"));

        eb = EmbedFactory.addLog(eb, logStatus, log);
        if (!active) eb = addRetryOption(eb);

        return eb;
    }

    private String getSpinningWheel(int i) {
        if (!progress[i]) return DiscordApiCollection.getInstance().getHomeEmojiById(401057220114251787L).getMentionTag();
        else return FRUITS_CONTAINER[fruits[i]];
    }

    private void manageEnd() throws IOException, SQLException {
        for(boolean b: progress) if (!b) return;

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
                for(int i=0; i<3; i++) progress[i] = true;
                manageEnd();
                message.edit(getEmbed());
                return;
            }

            for(int i=0; i<NUMBERS.length; i++) {
                String str = NUMBERS[i];
                if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(str) && !progress[i]) {
                    progress[i] = true;
                    manageEnd();
                    message.edit(getEmbed());
                    break;
                }
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {
        if (active) {
            for (int i = 0; i < 3; i++) progress[i] = true;
            manageEnd();
            message.edit(getEmbed());
        }
    }
}
