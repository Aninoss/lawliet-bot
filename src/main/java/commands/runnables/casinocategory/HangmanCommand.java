package commands.runnables.casinocategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnForwardedRecievedListener;
import commands.listeners.OnReactionAddListener;
import commands.runnables.CasinoAbstract;
import constants.Category;
import constants.LogStatus;
import constants.Permission;
import constants.Response;
import core.EmbedFactory;
import core.FileManager;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "hangman",
        emoji = "\uD83D\uDD21",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        executableWithoutArgs = true,
        aliases = {"hm"}
)
public class HangmanCommand extends CasinoAbstract implements OnForwardedRecievedListener, OnReactionAddListener {

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
            Random r = new Random();
            List<String> wordList = FileManager.readInList(new File("recourses/hangman_" + getLocale().getDisplayName() + ".txt"));
            answer = wordList.get(r.nextInt(wordList.size()));
            first = true;
            health = MAX_HEALTH;
            progress = new boolean[answer.length()];
            used = new ArrayList<>();
            message = event.getChannel().sendMessage(getEmbed(false)).get();
            message.addReaction("❌");

            return true;
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

        if (coinsInput != 0) eb.setFooter(TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));

        eb = EmbedUtil.addLog(eb, logStatus, log);
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

    private void onAbort() throws IOException, SQLException, ExecutionException {
        logStatus = LogStatus.LOSE;
        onLose();
        log = getString("abort");
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
    public Response onForwardedRecieved(MessageCreateEvent event) throws Throwable {
        String input = event.getMessage().getContent().toUpperCase()
                .replace("Ä", "AE")
                .replace("Ö", "OE")
                .replace("Ü", "UE")
                .replace("ß", "SS");
        if (input.length() == 0) return null;

        if (!used.contains(input)) {
            if (input.length() == 1) {
                used.add(input);
                boolean successful = false;
                for (int i = 0; i < answer.length(); i++) {
                    if (answer.charAt(i) == input.toUpperCase().charAt(0)) {
                        progress[i] = true;
                        successful = true;
                    }
                }

                if (!successful) onWrong(input);
                else onRight(input);
                event.getMessage().delete();
                return successful ? Response.TRUE : Response.FALSE;
            } else return null;
        } else {
            logStatus = LogStatus.FAILURE;
            log = getString("used", input);
            message.edit(getEmbed(false));
        }

        event.getMessage().delete();
        return Response.FALSE;
    }

    private void onWrong(String input) throws IOException, SQLException, ExecutionException {
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

    private void onRight(String input) throws IOException, SQLException, ExecutionException {
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
            message.edit(getEmbed(false));
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}
}
