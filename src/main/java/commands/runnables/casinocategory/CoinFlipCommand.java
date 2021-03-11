package commands.runnables.casinocategory;

import java.util.Locale;
import java.util.Random;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Category;
import constants.Emojis;
import core.EmbedFactory;
import core.TextManager;
import core.schedule.MainScheduler;
import core.utils.EmbedUtil;
import core.utils.JDAEmojiUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "coinflip",
        emoji = "\uD83D\uDCB0",
        executableWithoutArgs = true,
        aliases = { "coin", "coins", "cf", "cointoss", "flip" }
)
public class CoinFlipCommand extends CasinoAbstract {

    private final String[] EMOJIS = { "🇭", "🇹" };
    private final int[] selection = { -1, -1 };

    public CoinFlipCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);
    }

    @Override
    public String[] onGameStart(GuildMessageReceivedEvent event, String args) {
        //TODO: Without "onlyNumberAsArgs"?
        int coinSideSelection = getCoinValue(args);
        if (coinSideSelection >= 0) selection[0] = coinSideSelection;

        if (selection[0] != -1) {
            manageEnd();
            return new String[] {};
        }

        return EMOJIS;
    }

    @Override
    public boolean onReactionCasino(GenericGuildMessageReactionEvent event) {
        for (int i = 0; i < 2; i++) {
            if (JDAEmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), EMOJIS[i])) {
                selection[0] = i;
                drawMessage(draw());
                manageEnd();
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        eb.addField(getString("yourbet"), getChoiceString(getTextChannel().get(), 0), true);
        eb.addField(getString("yourthrow"), getChoiceString(getTextChannel().get(), 1), true);
        eb.addField(Emojis.EMPTY_EMOJI, getString("template", playerName, StringUtil.numToString(coinsInput)), false);

        if (selection[0] == -1) {
            eb.addField(Emojis.EMPTY_EMOJI, getString("expl", EMOJIS[0], EMOJIS[1]), false);
        }

        if (coinsInput != 0) {
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));
        }

        return eb;
    }

    private int getCoinValue(String args) {
        for (String word : args.toLowerCase().split(" ")) {
            if (word.equals("h")) return 0;
            if (word.equals("t")) return 1;
            if (word.startsWith("head")) return 0;
            if (word.startsWith("tail")) return 1;
        }

        return -1;
    }

    private String getChoiceString(TextChannel channel, int pos) {
        if (pos == 1 && selection[0] == -1) {
            return Emojis.EMPTY_EMOJI;
        }

        if (selection[0] != -1 && selection[1] == -1) {
            return Emojis.COUNTDOWN_3;
        }

        switch (selection[pos]) {
            case 0:
                return EMOJIS[0];
            case 1:
                return EMOJIS[1];
            default:
                return JDAEmojiUtil.getLoadingEmojiMention(channel);
        }
    }

    private void manageEnd() {
        if (selection[0] == -1) return;
        removeReactionListener();

        MainScheduler.getInstance().schedule(3000, "coinflip_cputhrow", () -> {
            selection[1] = new Random().nextInt(2);
            drawMessage(draw());

            MainScheduler.getInstance().schedule(1000, "coinflip_results", () -> {
                if (selection[0] == selection[1]) {
                    win();
                } else {
                    lose();
                }

                drawMessage(draw());
            });
        });
    }

}
