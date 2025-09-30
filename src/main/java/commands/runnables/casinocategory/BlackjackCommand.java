package commands.runnables.casinocategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

@CommandProperties(
        trigger = "blackjack",
        emoji = "\uD83C\uDCCF",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "bj" }
)
public class BlackjackCommand extends CasinoAbstract {

    private enum PlayerType { PLAYER, DEALER }

    private static final int TIME_BETWEEN_EVENTS = 2500;
    private static final int TIME_BEFORE_END = 1500;
    private static final String BUTTON_ID_HIT = "hit";
    private static final String BUTTON_ID_STAND = "stand";

    private final HashMap<Integer, ArrayList<GameCard>> gameCards = new HashMap<>();
    private PlayerType cardRecentDrawn = null;
    private boolean turnForPlayer = true;

    public BlackjackCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true);
    }

    @Override
    public boolean onGameStart(CommandEvent event, String args) {
        for (PlayerType value : PlayerType.values()) {
            ArrayList<GameCard> cards = getCardsForPlayer(value);
            for (int i = 0; i < 2 - value.ordinal(); i++) {
                cards.add(new GameCard());
            }
        }

        return true;
    }

    @Override
    public boolean onButtonCasino(ButtonInteractionEvent event) throws Throwable {
        if (turnForPlayer) {
            if (event.getComponentId().equals(BUTTON_ID_HIT)) {
                getCardsForPlayer(PlayerType.PLAYER).add(new GameCard());
                cardRecentDrawn = PlayerType.PLAYER;
                setLog(LogStatus.SUCCESS, getString("getcard", 0));

                if (getCardsValue(PlayerType.PLAYER) > 21) {
                    turnForPlayer = false;
                    schedule(Duration.ofMillis(TIME_BEFORE_END), () -> {
                        lose(event.getMember(), true);
                        setLog(LogStatus.LOSE, getString("toomany", 0));
                        drawMessage(draw(event.getMember())).exceptionally(ExceptionLogger.get());
                    });
                }
                return true;
            } else if (event.getComponentId().equals(BUTTON_ID_STAND)) {
                turnForPlayer = false;
                deregisterListeners();

                setLog(LogStatus.SUCCESS, getString("stopcard", 0));
                onCPUTurn(event.getMember());
                return true;
            }
        }
        return false;
    }

    private void onCPUTurn(Member member) {
        poll(Duration.ofMillis(TIME_BETWEEN_EVENTS), () -> onCPUTurnStep(member));
    }

    private boolean onCPUTurnStep(Member member) {
        getCardsForPlayer(PlayerType.DEALER).add(new GameCard());
        setLog(LogStatus.SUCCESS, getString("getcard", 1));
        cardRecentDrawn = PlayerType.DEALER;
        drawMessage(draw(member)).exceptionally(ExceptionLogger.get());

        int cardsValue = getCardsValue(PlayerType.DEALER);
        if (cardsValue >= 17) {
            if (cardsValue <= 21) {
                schedule(Duration.ofMillis(TIME_BETWEEN_EVENTS), () -> {
                    setLog(LogStatus.SUCCESS, getString("stopcard", 1));
                    drawMessage(draw(member)).exceptionally(ExceptionLogger.get());

                    schedule(Duration.ofMillis(TIME_BEFORE_END), () -> {
                        HashMap<PlayerType, Boolean> hasBlackJackMap = new HashMap<>();
                        for (PlayerType playerType : PlayerType.values()) {
                            hasBlackJackMap.put(
                                    playerType,
                                    getCardsValue(playerType) == 21 && getCardsForPlayer(playerType).size() == 2
                            );
                        }

                        if (hasBlackJackMap.get(PlayerType.PLAYER) && !hasBlackJackMap.get(PlayerType.DEALER)) {
                            win(member);
                            setLog(LogStatus.WIN, getString("blackjack", 0));
                            drawMessage(draw(member)).exceptionally(ExceptionLogger.get());
                            return;
                        } else if (hasBlackJackMap.get(PlayerType.DEALER) && !hasBlackJackMap.get(PlayerType.PLAYER)) {
                            lose(member, true);
                            setLog(LogStatus.LOSE, getString("blackjack", 1));
                            drawMessage(draw(member)).exceptionally(ExceptionLogger.get());
                            return;
                        }

                        int[] points = {
                                21 - getCardsValue(PlayerType.PLAYER),
                                21 - getCardsValue(PlayerType.DEALER)
                        };

                        if (points[0] == points[1]) {
                            tie(member);
                            drawMessage(draw(member)).exceptionally(ExceptionLogger.get());
                        } else if (points[0] < points[1]) {
                            win(member);
                            setLog(LogStatus.WIN, getString("21", 0));
                            drawMessage(draw(member)).exceptionally(ExceptionLogger.get());
                        } else if (points[0] > points[1]) {
                            lose(member, true);
                            setLog(LogStatus.LOSE, getString("21", 1));
                            drawMessage(draw(member)).exceptionally(ExceptionLogger.get());
                        }
                    });
                });
            } else {
                schedule(Duration.ofMillis(TIME_BEFORE_END), () -> {
                    win(member);
                    setLog(LogStatus.WIN, getString("toomany", 1));
                    drawMessage(draw(member)).exceptionally(ExceptionLogger.get());
                });
            }
            return false;
        }

        return true;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(
                        getString("cards", false, getCardsValueString(PlayerType.PLAYER), StringUtil.escapeMarkdown(playerName)),
                        getCardsString(PlayerType.PLAYER, cardRecentDrawn == PlayerType.PLAYER),
                        true
                )
                .addField(
                        getString("cards", true, getCardsValueString(PlayerType.DEALER)),
                        getCardsString(PlayerType.DEALER, cardRecentDrawn == PlayerType.DEALER),
                        true
                );
        cardRecentDrawn = null;

        if (turnForPlayer) {
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_HIT, getString("hit")),
                    Button.of(ButtonStyle.SECONDARY, BUTTON_ID_STAND, getString("stand"))
            );
        }

        if (coinsInput != 0) {
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));
        }

        String key = turnForPlayer ? "tutorial" : "data";

        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString(key, playerName, StringUtil.numToString(coinsInput)), false);
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
                sb.append(Emojis.CARD_FADEIN[gameCard.getId()].getFormatted());
            } else {
                sb.append(Emojis.CARD[gameCard.getId()].getFormatted());
            }
        }
        sb.append(Emojis.ZERO_WIDTH_SPACE.getFormatted());
        return sb.toString();
    }

    private String getCardsValueString(PlayerType playerType) {
        int value = getCardsValue(playerType);
        if (getCardsForPlayer(playerType).size() == 2 && value == 21) {
            return getCommandLanguage().getTitle();
        } else {
            return String.valueOf(value);
        }
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
