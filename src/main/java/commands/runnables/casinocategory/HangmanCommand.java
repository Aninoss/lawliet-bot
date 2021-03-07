package commands.runnables.casinocategory;

import java.util.*;
import java.util.concurrent.ExecutionException;
import commands.listeners.CommandProperties;
import commands.listeners.OnMessageInputListener;
import commands.runnables.CasinoAbstract;
import constants.Category;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.FileManager;
import core.ResourceHandler;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "hangman",
        emoji = "\uD83D\uDD21",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        executableWithoutArgs = true,
        aliases = {"hm"}
)
public class HangmanCommand extends CasinoAbstract implements OnMessageInputListener, OnReactionAddListener {

    private String answer, log;
    private int health;
    private final int MAX_HEALTH = 6;
    private boolean[] progress;
    private LogStatus logStatus;
    private ArrayList<String> used;
    private boolean first;

    public HangmanCommand(Locale locale, String prefix) {
        super(locale, prefix);
        winMultiplicator = 1;
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (onGameStart(event, followedString)) {
            try {
                Random r = new Random();
                List<String> wordList = FileManager.readInList(ResourceHandler.getFileResource("data/resources/hangman_" + getLocale().getDisplayName() + ".txt"));
                answer = wordList.get(r.nextInt(wordList.size()));
                first = true;
                health = MAX_HEALTH;
                progress = new boolean[answer.length()];
                used = new ArrayList<>();
                message = event.getChannel().sendMessage(getEmbed(false)).get();
                message.addReaction("❌").get();

                return true;
            } catch (Throwable e) {
                handleError(e, event.getServerTextChannel().get());
                return false;
            }
        }
        return false;
    }

    private EmbedBuilder getEmbed(boolean wrong) {
        String key = "template_ongoing";
        if (first) {
            key = "template_start";
            first = false;
        }
        if (!active && !won) key = "template_end";
        if (!active && won) key = "template";

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(key,
                player.getDisplayName(server),
                StringUtil.numToString(coinsInput),
                getProgress(),
                StringUtil.generateHeartBar(health, MAX_HEALTH, wrong),
                answer,
                getUsedString()));

        if (coinsInput != 0) EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));

        eb = EmbedUtil.addLog(eb, logStatus, log);
        if (!active) eb = addRetryOption(eb);

        return eb;
    }

    private String getProgress() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<progress.length; i++) {
            if (progress[i]) sb.append(answer.charAt(i));
            else sb.append('-');
        }

        return sb.toString();
    }

    private void onAbort() throws ExecutionException {
        logStatus = LogStatus.LOSE;
        onLose();
        log = getString("abort");
    }

    private String getUsedString() {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < used.size(); i++) {
            String str = used.get(i);
            if (i != 0) sb.append(", ");
            sb.append(str);
        }

        return sb.toString();
    }

    @Override
    public Response onForwardedRecieved(MessageCreateEvent event) throws Throwable {
        String input = event.getMessage().getContent().toUpperCase();

        if (input.length() != 1) {
            if (!stringCouldMatch(input)) //if input can't be right return
                return null;

            event.getMessage().delete();
            used.add(input);
            if (answer.equals(input)) { //if input is right win game
                Arrays.fill(progress, true);
                onRight(input);
                return Response.TRUE;
            } else { //input is wrong
                onWrong(input);
                return Response.FALSE;
            }
        }

        char inputChar = input.charAt(0);

        if (!Character.isLetter(inputChar)) return null;

        if (!used.contains(String.valueOf(inputChar))) {
            used.add(String.valueOf(inputChar));
            boolean successful = false;
            for (int i = 0; i < answer.length(); i++) {
                if (answer.charAt(i) == inputChar) {
                    progress[i] = true;
                    successful = true;
                }
            }

            if (!successful) onWrong(String.valueOf(inputChar));
            else onRight(String.valueOf(inputChar));
            event.getMessage().delete();
            return successful ? Response.TRUE : Response.FALSE;
        } else {
            logStatus = LogStatus.FAILURE;
            log = getString("used", input);
            message.edit(getEmbed(false));
        }

        event.getMessage().delete();
        return Response.FALSE;
    }

    private boolean stringCouldMatch(String input) { //input should be uppercase
        if (input.length() != answer.length()) //string can't be right
            return false;

        char[] inputChars = input.toCharArray();
        char[] answerChars = answer.toCharArray();

        for (int i = 0; i < inputChars.length; i++) {
            if (!Character.isLetter(inputChars[i])) return false;
            if (progress[i]) { //char has been solved
                if (inputChars[i] != answerChars[i])  //string can't be right
                    return false;
            } else { //hasn't been solved
                if (used.contains(String.valueOf(inputChars[i]))) //string can't be right
                    return false;
            }
        }
        return true;
    }

    private void onWrong(String input) throws ExecutionException {
        health --;

        if (health > 0) {
            logStatus = LogStatus.FAILURE;
            log = getString("wrong", input);
        } else {
            logStatus = LogStatus.LOSE;
            log = TextManager.getString(getLocale(), TextManager.GENERAL, "lost");
            onLose();
        }

        message.edit(getEmbed(true));
    }

    private void onRight(String input) throws ExecutionException {
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
            log = TextManager.getString(getLocale(), TextManager.GENERAL, "won");
            winMultiplicator = (double) health / (double) MAX_HEALTH;
            onWin();
        }

        message.edit(getEmbed(false));
    }

    @Override
    public Message getForwardedMessage() {
        return message;
    }

    @Override
    public void onForwardedTimeOut() throws Throwable {}

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!active) {
            onReactionAddRetry(event);
            return;
        }

        if (event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equals("❌")) {
            onAbort();
            message.edit(getEmbed(false));
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {
        if (active) {
            onAbort();
        }
    }
}
