package commands.runnables.casinocategory;

import commands.Category;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoMultiplayerAbstract;
import constants.Emojis;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicMember;
import jakarta.validation.constraints.NotNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@CommandProperties(
        trigger = "matchingcards",
        emoji = "🃏",
        patreonRequired = true,
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"matching", "cards"}
)
public class MatchingCardsCommand extends CasinoMultiplayerAbstract {

    private final static int MAX_CARDS_PER_HAND = 25;
    private final static int TIMEOUT = 30;

    private final ArrayList<ArrayList<Card>> playerCards = new ArrayList<>();
    private Card activeCard;
    private int currentPlayer = -1;
    private int direction = 1;
    private int actionId = 0;
    private boolean canDrawCards = true;
    private int attackValue = 0;

    public MatchingCardsCommand(Locale locale, String prefix) {
        super(locale, prefix, 2, 4, true);
    }

    @Override
    public void onGameStart(List<AtomicMember> players) {
        for (int i = 0; i < players.size(); i++) {
            ArrayList<Card> cards = new ArrayList<>();
            for (int i1 = 0; i1 < 7; i1++) {
                cards.add(new Card(false, getPlayerList().size()));
            }
            cards.sort(Card::compareTo);
            playerCards.add(cards);
        }

        activeCard = new Card(true, getPlayerList().size());
        currentPlayer = ThreadLocalRandom.current().nextInt(players.size()) - 1;
        startNewTurn(false);
    }

    @Override
    public synchronized boolean onButtonCasino(ButtonInteractionEvent event, int player) {
        if (!canDrawCards) {
            startNewTurn(false);
            return true;
        }

        drawCards(false);
        return true;
    }

    @Override
    public boolean onStringSelectMenuCasino(StringSelectInteractionEvent event, int player) {
        if (currentPlayer != player) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString("error_notyourturn")).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }

        ArrayList<Card> cards = playerCards.get(currentPlayer);
        Card card = cards.get(Integer.parseInt(event.getSelectedOptions().get(0).getValue()));
        if (!playCard(card)) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString(attackValue > 0 ? "error_attack_incompatible" : "error_incompatible", activeCard.getLabel(getLocale()))).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }
        return true;
    }

    @Override
    public EmbedBuilder drawCasino() {
        List<AtomicMember> players = getPlayerList();

        StringBuilder sb = new StringBuilder();
        String directionEmoji = direction == 1 ? "⬇️" : "⬆️";
        for (int i = 0; i < players.size(); i++) {
            String line = getString("playerslot",
                    currentPlayer == i ? directionEmoji : Emojis.FULL_SPACE_EMOTE.getFormatted(),
                    players.get(i).getName(getLocale()),
                    String.valueOf(playerCards.get(i).size())
            );
            sb.append(line)
                    .append("\n");
        }

        return EmbedFactory.getEmbedDefault(this, getString("main", activeCard.getEmoji().getFormatted(), activeCard.getLabel(getLocale()), sb.toString()));
    }

    @Override
    public Collection<? extends LayoutComponent> generatePlayerActionRows(int player) {
        ArrayList<ActionRow> actionRows = new ArrayList<>();
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create("play_card")
                .setPlaceholder(getString("selectmenu"))
                .setRequiredRange(1, 1);

        for (int i = 0; i < playerCards.get(player).size(); i++) {
            Card card = playerCards.get(player).get(i);
            selectMenuBuilder.addOption(card.getLabel(getLocale()), String.valueOf(i), card.getDescription(getLocale()), card.getEmoji());
        }
        actionRows.add(ActionRow.of(selectMenuBuilder.build()));

        if (currentPlayer == player) {
            int drawNumber = Math.min(MAX_CARDS_PER_HAND, Math.max(1, attackValue));
            Button button = Button.of(ButtonStyle.PRIMARY, "button", getString(canDrawCards ? "button_drawcards" : "button_endturn", drawNumber != 1, String.valueOf(drawNumber)));
            actionRows.add(ActionRow.of(button));
        }

        return actionRows;
    }

    private boolean playCard(Card card) {
        if (!card.compatible(activeCard) || (attackValue > 0 && card.value != activeCard.value)) {
            return false;
        }

        ArrayList<Card> cards = playerCards.get(currentPlayer);
        cards.remove(card);
        activeCard = card;

        if (cards.isEmpty()) {
            end(List.of(currentPlayer));
        } else {
            startNewTurn(true);
        }
        return true;
    }

    private void drawCards(boolean startNewTimeout) {
        int number = Math.min(MAX_CARDS_PER_HAND, Math.max(1, attackValue));
        ArrayList<Card> cards = playerCards.get(currentPlayer);

        while (cards.size() + number > MAX_CARDS_PER_HAND) {
            cards.remove(ThreadLocalRandom.current().nextInt(cards.size()));
        }
        for (int i = 0; i < number; i++) {
            Card newCard = new Card(false, getPlayerList().size());
            cards.add(newCard);
        }
        cards.sort(Card::compareTo);

        setLog(null, getString("log_draw", number != 1, getPlayerList().get(currentPlayer).getName(getLocale()), String.valueOf(number)));
        canDrawCards = false;
        attackValue = 0;

        if (startNewTimeout) {
            scheduleTimeout(5);
        }
    }

    private void startNewTurn(boolean checkActiveCard) {
        int players = getPlayerList().size();
        if (checkActiveCard) {
            switch (activeCard.value) {
                case Card.VALUE_REVERSE -> direction *= -1;
                case Card.VALUE_DRAW_2 -> attackValue += 2;
                case Card.VALUE_DRAW_4 -> attackValue += 4;
            }
        }

        currentPlayer += direction * (checkActiveCard && activeCard.value == Card.VALUE_SKIP ? 2 : 1);
        if (currentPlayer >= players) {
            currentPlayer -= players;
        }
        if (currentPlayer < 0) {
            currentPlayer += players;
        }

        canDrawCards = true;
        setLog(null, getString("log_playerturn", getPlayerList().get(currentPlayer).getName(getLocale()), String.valueOf(TIMEOUT)));
        scheduleTimeout(TIMEOUT);
    }

    private void scheduleTimeout(int seconds) {
        int newActionId = ThreadLocalRandom.current().nextInt();
        this.actionId = newActionId;
        schedule(Duration.ofSeconds(seconds), () -> onTimeout(newActionId));
    }

    private void onTimeout(int actionId) {
        if (actionId != this.actionId || getStatus() != Status.PLAYING) {
            return;
        }

        ArrayList<Card> cards = playerCards.get(currentPlayer);
        for (Card card : cards) {
            if (playCard(card)) {
                redraw();
                return;
            }
        }

        if (canDrawCards) {
            drawCards(true);
            redraw();
            return;
        }

        startNewTurn(false);
        redraw();
    }


    private static class Card implements Comparable<Card> {

        private final static String[] COLORS = new String[]{"🟥", "🟦", "🟩", "🟨"};
        private final static int VALUE_SKIP = 10;
        private final static int VALUE_REVERSE = 11;
        private final static int VALUE_DRAW_2 = 12;
        private final static int VALUE_DRAW_4 = 13;

        private final int color;
        private int value;

        public Card(boolean basicOnly, int players) {
            if (basicOnly) {
                this.color = ThreadLocalRandom.current().nextInt(4);
                this.value = 1 + ThreadLocalRandom.current().nextInt(9);
            } else {
                if (ThreadLocalRandom.current().nextInt(12) == 0) {
                    this.color = -1;
                    this.value = 0;
                } else {
                    this.color = ThreadLocalRandom.current().nextInt(4);
                    this.value = 1 + ThreadLocalRandom.current().nextInt(VALUE_DRAW_4);
                }
            }

            if (players <= 2 && this.value == VALUE_REVERSE) {
                this.value = VALUE_SKIP;
            }
        }

        @Override
        public int compareTo(@NotNull Card o) {
            if (this.color != o.color) {
                return this.color - o.color;
            } else {
                return this.value - o.value;
            }
        }

        public boolean compatible(Card o) {
            return this.color == -1 || o.color == -1 || this.color == o.color || this.value == o.value;
        }

        public Emoji getEmoji() {
            if (this.color == -1) {
                return Emoji.fromUnicode("🃏");
            } else {
                return Emoji.fromUnicode(COLORS[this.color]);
            }
        }

        public String getLabel(Locale locale) {
            if (this.value >= 1 && this.value <= 9) {
                return String.valueOf(this.value);
            } else {
                return TextManager.getString(locale, Category.CASINO, "matchingcards_card_" + this.value);
            }
        }

        public String getDescription(Locale locale) {
            if (this.value >= 1 && this.value <= 9) {
                return null;
            } else {
                return TextManager.getString(locale, Category.CASINO, "matchingcards_card_desc_" + this.value);
            }
        }

    }

}