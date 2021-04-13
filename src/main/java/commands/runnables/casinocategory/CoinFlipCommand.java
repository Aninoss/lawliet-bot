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
import core.utils.EmojiUtil;
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

    private final String[] EMOJIS = { "🇭", "🇹", Emojis.X };
    private final int[] selection = { -1, -1 };

    public CoinFlipCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false);
    }

    @Override
    public String[] onGameStart(GuildMessageReceivedEvent event, String args) {
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
            if (EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), EMOJIS[i])) {
                selection[0] = i;
                manageEnd();
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        eb.addField(getString("yourbet"), getChoiceString(getTextChannel().get(), 0) + Emojis.ZERO_WIDTH_SPACE, true);
        eb.addField(getString("yourthrow"), getChoiceString(getTextChannel().get(), 1) + Emojis.ZERO_WIDTH_SPACE, true);
        eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("template", playerName, StringUtil.numToString(coinsInput)), false);

        if (selection[0] == -1) {
            eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("expl", EMOJIS[0], EMOJIS[1]), false);
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
            return "";
        }

        if (selection[0] != -1 && selection[1] == -1) {
            return Emojis.COUNTDOWN_3;
        }

        return switch (selection[pos]) {
            case 0 -> EMOJIS[0];
            case 1 -> EMOJIS[1];
            default -> EmojiUtil.getLoadingEmojiMention(channel);
        };
    }

    private void manageEnd() {
        if (selection[0] == -1) return;
        deregisterListenersWithReactions();

        MainScheduler.getInstance().schedule(3000, "coinflip_cputhrow", () -> {
            selection[1] = new Random().nextBoolean() ? 1 : 0;
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
