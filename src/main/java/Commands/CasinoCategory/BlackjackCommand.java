package Commands.CasinoCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnReactionAddListener;
import Commands.CasinoAbstract;
import Constants.Category;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Settings;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.TextManager;
import Core.Utils.StringUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "blackjack",
        emoji = "\uD83C\uDCCF",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        executable = true,
        aliases = {"bj"}
)
public class BlackjackCommand extends CasinoAbstract implements OnReactionAddListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(BlackjackCommand.class);

    private String log;
    private LogStatus logStatus;
    private String[] EMOJIS = {"\uD83D\uDCE5", "✋"};
    private ArrayList<GameCard>[] gameCards;
    private final int TIME_BETWEEN_EVENTS = 2500;
    private final int TIME_BEFORE_END = 1500;
    private boolean block;
    private boolean finished;

    public BlackjackCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {
            winMultiplicator = 1;
            block = false;
            finished = false;
            gameCards = new ArrayList[2];

            gameCards[0] = new ArrayList<>();
            for(int i=0; i<2; i++) gameCards[0].add(new GameCard());

            gameCards[1] = new ArrayList<>();
            for(int i=0; i<1; i++) gameCards[1].add(new GameCard());

            message = event.getChannel().sendMessage(getEmbed(-1)).get();
            for (String str : EMOJIS) message.addReaction(str);

            return true;
        }
        return false;
    }

    private EmbedBuilder getEmbed(int playerNewCard) {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .addField(getString("cards", false, String.valueOf(getCardSize(0)), server.getDisplayName(player)), getCards(0, playerNewCard == 0),true)
                .addField(getString("cards", true, String.valueOf(getCardSize(1))),getCards(1, playerNewCard == 1),true);

        if (coinsInput != 0) eb.setFooter(TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));

        String key = "tutorial";
        if (finished) key = "data";

        eb.addField(Settings.EMPTY_EMOJI, getString(key, server.getDisplayName(player), StringUtil.numToString(getLocale(), coinsInput)), false);

        eb = EmbedFactory.addLog(eb, logStatus, log);
        if (!active) eb = addRetryOption(eb);

        return eb;
    }

    private String getCards(int i, boolean newCard) {
        StringBuilder sb = new StringBuilder();
        for(int j = 0; j < gameCards[i].size(); j++) {
            GameCard gameCard = gameCards[i].get(j);
            if (j == gameCards[i].size() - 1 && newCard)
                sb.append(DiscordApiCollection.getInstance().getHomeEmojiByName(String.format("card_%d_get", gameCard.getId())).getMentionTag());
            else
                sb.append(DiscordApiCollection.getInstance().getHomeEmojiByName(String.format("card_%d", gameCard.getId())).getMentionTag());
        }
        sb.append(Settings.EMPTY_EMOJI);

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
                message.edit(getEmbed(0));

                if (getCardSize(0) > 21) {
                    block = true;
                    Thread.sleep(TIME_BEFORE_END);
                    finished = true;
                    onLose();

                    logStatus = LogStatus.LOSE;
                    log = getString("toomany", 0);
                    message.edit(getEmbed(-1));
                }
            } else if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(EMOJIS[1])) {
                finished = true;
                removeReactionListener(getReactionMessage());

                logStatus = LogStatus.SUCCESS;
                log = getString("stopcard", 0);
                message.edit(getEmbed(-1));

                try {
                    onCPUTurn();
                } catch (ExecutionException e) {
                    onLose();
                    LOGGER.error("Error in black jack", e);
                }
            }
        }
    }

    private void onCPUTurn() throws ExecutionException, InterruptedException {
            while (true) {
                Thread.sleep(TIME_BETWEEN_EVENTS);

                gameCards[1].add(new GameCard());
                logStatus = LogStatus.SUCCESS;
                log = getString("getcard", 1);
                message.edit(getEmbed(1)).get();

                if (getCardSize(1) >= 17) {
                    if (getCardSize(1) <= 21) {
                        Thread.sleep(TIME_BETWEEN_EVENTS);
                        logStatus = LogStatus.SUCCESS;
                        log = getString("stopcard", 1);
                        message.edit(getEmbed(-1));

                        Thread.sleep(TIME_BEFORE_END);

                        boolean[] blackjack = new boolean[2];
                        for(int i=0; i<2; i++) if (getCardSize(i) == 21 && gameCards[i].size() == 2) blackjack[i] = true;

                        if (blackjack[0] && !blackjack[1]) {
                            onWin();
                            logStatus = LogStatus.WIN;
                            log = getString("blackjack", 0);
                            message.edit(getEmbed(-1)).get();
                            return;
                        }

                        else if (blackjack[1] && !blackjack[0]) {
                            onLose();
                            logStatus = LogStatus.LOSE;
                            log = getString("blackjack", 1);
                            message.edit(getEmbed(-1)).get();
                            return;
                        }

                        int[] points = {21-getCardSize(0), 21-getCardSize(1)};

                        if (points[0] == points[1]) {
                            onGameEnd();
                            logStatus = LogStatus.FAILURE;
                            log = getString("draw");
                            message.edit(getEmbed(-1)).get();
                            return;
                        }

                        else if (points[0] < points[1]) {
                            onWin();
                            logStatus = LogStatus.WIN;
                            log = getString("21", 0);
                            message.edit(getEmbed(-1)).get();
                            return;
                        }

                        else if (points[0] > points[1]) {
                            onLose();
                            logStatus = LogStatus.LOSE;
                            log = getString("21", 1);
                            message.edit(getEmbed(-1)).get();
                            return;
                        }
                    } else {
                        Thread.sleep(TIME_BEFORE_END);

                        onWin();
                        logStatus = LogStatus.WIN;
                        log = getString("toomany", 1);
                        message.edit(getEmbed(-1));
                        return;
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
            logStatus = LogStatus.LOSE;
            log = getString("abort");
            onLose();
        }
    }

    public static class GameCard {

        private int id;
        private int value;
        private boolean ace;

        public GameCard() {
            ace = false;
            value = 0;

            Random r = new Random();
            id = r.nextInt(13);

            if (id <= 8) {
                value = id + 2;
            } else if (id == 12) {
                ace = true;
                value = 11;
            } else {
                value = 10;
            }
        }

        public int getValue() {
            return value;
        }

        public int getId() { return id; }

        public boolean isAce() {
            return ace;
        }
    }

}
