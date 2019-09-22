package Commands.Casino;

import CommandListeners.CommandProperties;
import CommandListeners.onReactionAddListener;
import CommandListeners.onRecievedListener;
import Constants.LogStatus;
import Constants.Permission;
import General.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;

@CommandProperties(
    trigger = "blackjack",
    emoji = "\uD83C\uDCCF",
    thumbnail = "http://icons.iconarchive.com/icons/flat-icons.com/flat/128/Coins-icon.png",
    executable = true
)
public class BlackjackCommand extends Casino implements onRecievedListener, onReactionAddListener {

    private String log;
    private LogStatus logStatus;
    private String[] EMOJIS = {"\uD83D\uDCE5", "✋"};
    private ArrayList<GameCard>[] gameCards;
    private final int TIME_BETWEEN_EVENTS = 2500;
    private final int TIME_BEFORE_END = 1000;
    private boolean block;
    private boolean finished;

    public BlackjackCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {
            winMultiplicator = 1;
            block = false;
            finished = false;
            gameCards = new  ArrayList[2];

            gameCards[0] = new ArrayList<GameCard>();
            for(int i=0; i<2; i++) gameCards[0].add(new GameCard());

            gameCards[1] = new ArrayList<GameCard>();
            for(int i=0; i<1; i++) gameCards[1].add(new GameCard());

            message = event.getChannel().sendMessage(getEmbed()).get();
            for (String str : EMOJIS) message.addReaction(str);

            return true;
        }
        return false;
    }

    private EmbedBuilder getEmbed() throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .addField(getString("cards", false, String.valueOf(getCardSize(0)), server.getDisplayName(player)), getCards(0),true)
                .addField(getString("cards", true, String.valueOf(getCardSize(1))),getCards(1),true);

        if (coinsInput != 0) eb.setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "casino_footer"));

        String key = "tutorial";
        if (finished) key = "data";

        eb.addField(Tools.getEmptyCharacter(), getString(key, server.getDisplayName(player), Tools.numToString(getLocale(), coinsInput)), false);

        eb = EmbedFactory.addLog(eb, logStatus, log);
        if (!active) eb = addRetryOption(eb);

        return eb;
    }

    private String getCards(int i) {
        StringBuilder sb = new StringBuilder();
        for(GameCard gameCard: gameCards[i]) {
            sb.append(gameCard.getEmoji());
        }

        return sb.toString();
    }

    private int getCardSize(int i) {
        int n = 0;
        int aces = 0;
        for (GameCard gameCard: gameCards[i]) {
            n += gameCard.getValue();
            if (gameCard.isAce()) aces++;
        }

        while (aces > 0 && n > 21) {
            n -= 10;
            aces--;
        }

        return n;
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!active) {
            onReactionAddRetry(event);
            return;
        }

        if (!block && event.getEmoji().isUnicodeEmoji()) {
            if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(EMOJIS[0])) {
                gameCards[0].add(new GameCard());
                logStatus = LogStatus.SUCCESS;
                log = getString("getcard", 0);
                message.edit(getEmbed()).get();

                if (getCardSize(0) > 21) {
                    block = true;
                    Thread.sleep(TIME_BEFORE_END);
                    finished = true;
                    onLose();

                    logStatus = LogStatus.LOSE;
                    log = getString("toomany", 0);
                    message.edit(getEmbed()).get();
                }
            } else if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(EMOJIS[1])) {
                finished = true;
                removeReactionListener(getReactionMessage());

                logStatus = LogStatus.SUCCESS;
                log = getString("stopcard", 0);
                message.edit(getEmbed());

                onCPUTurn();
            }
        }
    }

    private void onCPUTurn() {
        try {
            while (true) {
                Thread.sleep(TIME_BETWEEN_EVENTS);

                gameCards[1].add(new GameCard());
                logStatus = LogStatus.SUCCESS;
                log = getString("getcard", 1);
                message.edit(getEmbed()).get();

                if (getCardSize(1) >= 17) {
                    if (getCardSize(1) <= 21) {
                        Thread.sleep(TIME_BETWEEN_EVENTS);
                        logStatus = LogStatus.SUCCESS;
                        log = getString("stopcard", 1);
                        message.edit(getEmbed());

                        Thread.sleep(TIME_BEFORE_END);

                        boolean[] blackjack = new boolean[2];
                        for(int i=0; i<2; i++) if (getCardSize(i) == 21 && gameCards[i].size() == 2) blackjack[i] = true;

                        if (blackjack[0] && !blackjack[1]) {
                            onWin();
                            logStatus = LogStatus.WIN;
                            log = getString("blackjack", 0);
                            message.edit(getEmbed()).get();
                            return;
                        }

                        else if (blackjack[1] && !blackjack[0]) {
                            onLose();
                            logStatus = LogStatus.LOSE;
                            log = getString("blackjack", 1);
                            message.edit(getEmbed()).get();
                            return;
                        }

                        int[] points = {21-getCardSize(0), 21-getCardSize(1)};

                        if (points[0] == points[1]) {
                            onGameEnd();
                            logStatus = LogStatus.FAILURE;
                            log = getString("draw");
                            message.edit(getEmbed()).get();
                            return;
                        }

                        else if (points[0] < points[1]) {
                            onWin();
                            logStatus = LogStatus.WIN;
                            log = getString("21", 0);
                            message.edit(getEmbed()).get();
                            return;
                        }

                        else if (points[0] > points[1]) {
                            onLose();
                            logStatus = LogStatus.LOSE;
                            log = getString("21", 1);
                            message.edit(getEmbed()).get();
                            return;
                        }
                    } else {
                        Thread.sleep(TIME_BEFORE_END);

                        onWin();
                        logStatus = LogStatus.WIN;
                        log = getString("toomany", 1);
                        message.edit(getEmbed());
                        return;
                    }
                }
            }
        } catch (Throwable throwable) {
            ExceptionHandler.handleException(throwable, getLocale(), channel);
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {
        if (active) {
            logStatus = LogStatus.LOSE;
            log = getString("abort");
            onLose();
        }
    }
}
