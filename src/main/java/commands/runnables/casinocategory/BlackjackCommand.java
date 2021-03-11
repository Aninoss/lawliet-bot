package commands.runnables.casinocategory;

import java.util.ArrayList;
import java.util.HashMap;
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
        trigger = "blackjack",
        emoji = "\uD83C\uDCCF",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        aliases = { "bj" }
)
public class BlackjackCommand extends CasinoAbstract {

    private enum PlayerType { PLAYER, DEALER }

    private final String[] ACTION_EMOJIS = { "📥", "✋" };
    private final int TIME_BETWEEN_EVENTS = 2500;
    private final int TIME_BEFORE_END = 1500;

    private final HashMap<Integer, ArrayList<GameCard>> gameCards = new HashMap<>();
    private PlayerType cardRecentDrawn = null;
    private boolean turnForPlayer = true;

    public BlackjackCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true);
    }

    @Override
    public String[] onGameStart(GuildMessageReceivedEvent event, String args) {
        for (PlayerType value : PlayerType.values()) {
            ArrayList<GameCard> cards = getCardsForPlayer(value);
            for (int i = 0; i < 2; i++) {
                cards.add(new GameCard());
            }
        }

        return ACTION_EMOJIS;
    }

    @Override
    public boolean onReactionCasino(GenericGuildMessageReactionEvent event) {
        if (turnForPlayer) {
            if (event.getReactionEmote().getAsReactionCode().equals(ACTION_EMOJIS[0])) {
                getCardsForPlayer(PlayerType.PLAYER).add(new GameCard());
                setLog(LogStatus.SUCCESS, getString("getcard", 0));

                if (getCardsValue(PlayerType.PLAYER) > 21) {
                    turnForPlayer = false;
                    MainScheduler.getInstance().schedule(TIME_BEFORE_END, "blackjack_player_overdrew", () -> {
                        lose();
                        setLog(LogStatus.LOSE, getString("toomany", 0));
                        drawMessage(draw());
                    });
                }
                return true;
            } else if (event.getReactionEmote().getAsReactionCode().equals(ACTION_EMOJIS[1])) {
                turnForPlayer = false;
                removeReactionListener();

                setLog(LogStatus.SUCCESS, getString("stopcard", 0));
                drawMessage(draw());
                onCPUTurn();
                return true;
            }
        }
        return false;
    }

    private void onCPUTurn() {
        MainScheduler.getInstance().poll(TIME_BETWEEN_EVENTS, "blackjack_cpu", this::onCPUTurnStep);
    }

    private boolean onCPUTurnStep() {
        getCardsForPlayer(PlayerType.DEALER).add(new GameCard());
        setLog(LogStatus.SUCCESS, getString("getcard", 1));
        drawMessage(draw());

        int cardsValue = getCardsValue(PlayerType.DEALER);
        if (cardsValue >= 17) {
            if (cardsValue <= 21) {
                MainScheduler.getInstance().schedule(TIME_BETWEEN_EVENTS, "blackjack_cpu_stop", () -> {
                    setLog(LogStatus.SUCCESS, getString("stopcard", 1));
                    drawMessage(draw());

                    MainScheduler.getInstance().schedule(TIME_BEFORE_END, "blackjack_checkresults", () -> {
                        HashMap<PlayerType, Boolean> hasBlackJackMap = new HashMap<>();
                        for (PlayerType playerType : PlayerType.values()) {
                            hasBlackJackMap.put(playerType,
                                    getCardsValue(playerType) == 21 && getCardsForPlayer(playerType).size() == 2
                            );
                        }

                        if (hasBlackJackMap.get(PlayerType.PLAYER) && !hasBlackJackMap.get(PlayerType.DEALER)) {
                            win();
                            setLog(LogStatus.WIN, getString("blackjack", 0));
                            drawMessage(draw());
                            return;
                        } else if (hasBlackJackMap.get(PlayerType.DEALER) && !hasBlackJackMap.get(PlayerType.PLAYER)) {
                            lose();
                            setLog(LogStatus.LOSE, getString("blackjack", 1));
                            drawMessage(draw());
                            return;
                        }

                        int[] points = {
                                21 - getCardsValue(PlayerType.PLAYER),
                                21 - getCardsValue(PlayerType.DEALER)
                        };

                        if (points[0] == points[1]) {
                            endGame();
                            drawMessage(draw());
                        } else if (points[0] < points[1]) {
                            win();
                            setLog(LogStatus.WIN, getString("21", 0));
                            drawMessage(draw());
                        } else if (points[0] > points[1]) {
                            lose();
                            setLog(LogStatus.LOSE, getString("21", 1));
                            drawMessage(draw());
                        }
                    });
                });
                return false;
            } else {
                MainScheduler.getInstance().schedule(TIME_BEFORE_END, "blackjack_cpu_overdrew", () -> {
                    win();
                    setLog(LogStatus.WIN, getString("toomany", 1));
                    drawMessage(draw());
                });
                return false;
            }
        }

        return true;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(getString("cards", false, String.valueOf(getCardsValue(PlayerType.PLAYER)), playerName),
                        getCardsString(PlayerType.PLAYER, cardRecentDrawn == PlayerType.PLAYER),
                        true
                )
                .addField(getString("cards", true, String.valueOf(getCardsValue(PlayerType.DEALER))),
                        getCardsString(PlayerType.DEALER, cardRecentDrawn == PlayerType.DEALER),
                        true
                );
        cardRecentDrawn = null;

        if (coinsInput != 0)
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));

        String key = turnForPlayer ? "tutorial" : "data";

        eb.addField(Emojis.EMPTY_EMOJI, getString(key, playerName, StringUtil.numToString(coinsInput)), false);
        return eb;
    }

    private ArrayList<GameCard> getCardsForPlayer(PlayerType player) {
        return gameCards.computeIfAbsent(player.ordinal(), k -> new ArrayList<>());
    }

    private String getCardsString(PlayerType player, boolean newCardDrawn) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getCardsForPlayer(player).size(); i++) {
            GameCard gameCard = getCardsForPlayer(player).get(i);
            if (i == getCardsForPlayer(player).size() - 1 && newCardDrawn) {
                sb.append(Emojis.CARD_FADEIN[gameCard.getId()]);
            } else {
                sb.append(Emojis.CARD[gameCard.getId()]);
            }
        }
        sb.append(Emojis.EMPTY_EMOJI);
        return sb.toString();
    }

    private int getCardsValue(PlayerType playerType) {
        int n = 0;
        int aces = 0;
        for (GameCard gameCard : getCardsForPlayer(playerType)) {
            n += gameCard.getValue();
            if (gameCard.isAce()) aces++;
        }

        while (aces > 0 && n > 21) {
            n -= 10;
            aces--;
        }

        return n;
    }


    private static class GameCard {

        private final int id;
        private final int value;
        private final boolean ace;

        public GameCard() {
            Random r = new Random();
            id = r.nextInt(13);

            if (id <= 8) {
                ace = false;
                value = id + 2;
            } else if (id == 12) {
                ace = true;
                value = 11;
            } else {
                ace = false;
                value = 10;
            }
        }

        public int getValue() {
            return value;
        }

        public int getId() {
            return id;
        }

        public boolean isAce() {
            return ace;
        }

    }

}
