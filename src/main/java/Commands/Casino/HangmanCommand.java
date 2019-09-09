package Commands.Casino;

import CommandListeners.*;
import Constants.LogStatus;
import General.*;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class HangmanCommand extends Casino implements onRecievedListener, onForwardedRecievedListener, onReactionAddListener {
    private String answer, log;
    private int health;
    private final int MAX_HEALTH = 8;
    private boolean[] progress;
    private LogStatus logStatus;
    private ArrayList<String> used;
    private boolean first;

    public HangmanCommand() {
        super();
        botPermissions = 0;
        userPermissions = 0;
        trigger = "hangman";
        emoji = "\uD83D\uDD21";
        thumbnail = "http://icons.iconarchive.com/icons/flat-icons.com/flat/128/Pencil-icon.png";

        winMultiplicator = 1;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {
            Random r = new Random();
            List<String> wordList = FileManager.readInList(new File("recourses/hangman_" + locale.getDisplayName() + ".txt"));

            int n;
            do {
                n = r.nextInt(wordList.size());
                answer = wordList.get(n).toUpperCase();
            } while(answer.endsWith("S"));

            first = true;
            health = MAX_HEALTH;
            progress = new boolean[answer.length()];
            used = new ArrayList<>();
            message = event.getChannel().sendMessage(getEmbed()).get();
            message.addReaction("❌");

            showAllNonLetterSymbols();

            return true;
        }
        return false;
    }

    private EmbedBuilder getEmbed() throws Throwable {
        String key = "template_ongoing";
        if (first) {
            key = "template_start";
            first = false;
        }
        if (!active && !won) key = "template_end";
        if (!active && won) key = "template";

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString(key,
                player.getDisplayName(server),
                Tools.numToString(locale, coinsInput),
                getProgress(),
                String.valueOf(health),
                String.valueOf(MAX_HEALTH),
                answer,
                getUsedString()));

        if (coinsInput != 0) eb.setFooter(TextManager.getString(locale, TextManager.COMMANDS, "casino_footer"));

        eb = EmbedFactory.addLog(eb, logStatus, log);
        if (!active) eb = addRetryOption(eb);

        return eb;
    }

    private String getProgress() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<progress.length; i++) {
            //if (i > 0) sb.append(Tools.getEmptyCharacter());
            if (progress[i]) sb.append(answer.charAt(i));
            else sb.append('-');
        }

        return sb.toString();
    }

    private void onAbort() throws Throwable {
        logStatus = LogStatus.LOSE;
        onLose();
        log = getString("abort");
    }

    private void showAllNonLetterSymbols() {
        for(int i = 0; i < answer.length(); i++) {
            if (!Character.isLetter(answer.charAt(i))) progress[i] = true;
        }
    }

    private String getUsedString() {
        StringBuilder sb = new StringBuilder();

        for(int i=0; i<used.size(); i++) {
            String str = used.get(i);
            if (i != 0) sb.append(", ");
            sb.append(str);
        }

        return sb.toString();
    }

    @Override
    public boolean onForwardedRecieved(MessageCreateEvent event) throws Throwable {
        event.getMessage().delete();

        String input = event.getMessage().getContent().toUpperCase()
                .replace("Ä", "AE")
                .replace("Ö", "OE")
                .replace("Ü", "UE")
                .replace("ß", "SS");
        if (input.length() == 0) return false;

        if (!used.contains(input)) {
            used.add(input);
            if (input.length() == 1) {
                boolean successful = false;
                for (int i = 0; i < answer.length(); i++) {
                    if (answer.charAt(i) == input.toUpperCase().charAt(0)) {
                        progress[i] = true;
                        successful = true;
                    }
                }

                if (!successful) onWrong(input);
                else onRight(input);
                return successful;
            } else {
                boolean successful = input.equalsIgnoreCase(answer);
                if (successful)
                    for (int i = 0; i < answer.length(); i++)
                        progress[i] = true;

                if (!successful) onWrong(input);
                else onRight(input);
                return successful;
            }
        } else {
            logStatus = LogStatus.FAILURE;
            log = getString("used", input);
            message.edit(getEmbed());
        }

        return false;
    }

    private void onWrong(String input) throws Throwable {
        health --;

        if (health > 0) {
            logStatus = LogStatus.FAILURE;
            log = getString("wrong", input);
        } else {
            logStatus = LogStatus.LOSE;
            log = TextManager.getString(locale, TextManager.GENERAL, "lost");
            onLose();
        }

        message.edit(getEmbed());
    }

    private void onRight(String input) throws Throwable {
        boolean finished = true;
        for (boolean set : progress) {
            if (!set) {
                finished = false;
                break;
            }
        }

        if (!finished) {
            logStatus = LogStatus.SUCCESS;
            log = getString("right", input);
        } else {
            logStatus = LogStatus.WIN;
            log = TextManager.getString(locale, TextManager.GENERAL, "won");
            winMultiplicator = (double) health / (double) MAX_HEALTH;
            onWin();
        }

        message.edit(getEmbed());
    }

    @Override
    public Message getForwardedMessage() {
        return message;
    }

    @Override
    public void onForwardedTimeOut() throws Throwable {
        onAbort();
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!active) {
            onReactionAddRetry(event);
            return;
        }

        if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals("❌")) {
            onAbort();
            message.edit(getEmbed());
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}
}
