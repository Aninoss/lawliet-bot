package commands.runnables.casinocategory;

import java.io.IOException;
import java.util.*;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.Category;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.FileManager;
import core.LocalFile;
import core.TextManager;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "hangman",
        emoji = "\uD83D\uDD21",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "hm" }
)
public class HangmanCommand extends CasinoAbstract {

    private final int MAX_HEALTH = 6;

    private String answer;
    private int health = MAX_HEALTH;
    private boolean[] progress;
    private final ArrayList<String> used = new ArrayList<>();
    private boolean first = true;
    private boolean wrongAnswer = false;

    public HangmanCommand(Locale locale, String prefix) {
        super(locale, prefix, false, true);
    }

    @Override
    public boolean onGameStart(GuildMessageReceivedEvent event, String args) throws IOException {
        Random r = new Random();
        List<String> wordList = FileManager.readInList(new LocalFile(LocalFile.Directory.RESOURCES, "hangman_" + getLocale().getDisplayName() + ".txt"));
        answer = wordList.get(r.nextInt(wordList.size()));
        progress = new boolean[answer.length()];
        setButtons(BUTTON_CANCEL);
        return true;
    }

    @Override
    public boolean onButtonCasino(ButtonClickEvent event) throws Throwable {
        return true;
    }

    @Override
    public EmbedBuilder drawCasino(String playerName, long coinsInput) {
        String key = "template_ongoing";
        if (first) {
            key = "template_start";
            first = false;
        }
        if (getStatus() != Status.ACTIVE && getStatus() != Status.WON) {
            key = "template_end";
        } else if (getStatus() == Status.WON) {
            key = "template";
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(
                key,
                playerName,
                StringUtil.numToString(coinsInput),
                getProgress(),
                StringUtil.generateHeartBar(health, MAX_HEALTH, wrongAnswer),
                answer,
                getUsedString()
        ));

        if (coinsInput != 0) {
            EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), Category.CASINO, "casino_footer"));
        }

        wrongAnswer = false;
        return eb;
    }

    private String getProgress() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < progress.length; i++) {
            if (progress[i]) {
                sb.append(answer.charAt(i));
            } else {
                sb.append('-');
            }
        }

        return sb.toString();
    }

    private String getUsedString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < used.size(); i++) {
            String str = used.get(i);
            if (i != 0) sb.append(", ");
            sb.append(str);
        }

        return sb.toString();
    }

    @Override
    public Response onMessageInputCasino(GuildMessageReceivedEvent event, String input) {
        input = input.toUpperCase();

        if (input.length() != 1) {
            if (!stringCouldMatch(input)) { //if input can't be right return
                return null;
            }

            used.add(input);
            if (answer.equals(input)) { //if input is right win game
                Arrays.fill(progress, true);
                onRight(event.getMember(), input);
            } else { //input is wrong
                onWrong(event.getMember(), input);
            }
            return Response.TRUE;
        }

        char inputChar = input.charAt(0);
        if (!Character.isLetter(inputChar)) {
            return null;
        }

        if (!used.contains(String.valueOf(inputChar))) {
            used.add(String.valueOf(inputChar));
            boolean successful = false;
            for (int i = 0; i < answer.length(); i++) {
                if (answer.charAt(i) == inputChar) {
                    progress[i] = true;
                    successful = true;
                }
            }

            if (!successful) {
                onWrong(event.getMember(), String.valueOf(inputChar));
            } else {
                onRight(event.getMember(), String.valueOf(inputChar));
            }
        } else {
            setLog(LogStatus.FAILURE, getString("used", input));
        }
        return Response.TRUE;
    }

    private void onWrong(Member member, String input) {
        health--;
        wrongAnswer = true;

        if (health > 0) {
            setLog(LogStatus.FAILURE, getString("wrong", input));
        } else {
            lose(member);
        }
    }

    private void onRight(Member member, String input) {
        boolean finished = true;
        for (boolean set : progress) {
            if (!set) {
                finished = false;
                break;
            }
        }

        if (!finished) {
            setLog(LogStatus.SUCCESS, getString("right", input));
        } else {
            win(member, (double) health / (double) MAX_HEALTH);
        }
    }

    private boolean stringCouldMatch(String input) { //input should be uppercase
        if (input.length() != answer.length()) //string can't be right
        {
            return false;
        }

        char[] inputChars = input.toCharArray();
        char[] answerChars = answer.toCharArray();

        for (int i = 0; i < inputChars.length; i++) {
            if (!Character.isLetter(inputChars[i])) return false;
            if (progress[i]) { //char has been solved
                if (inputChars[i] != answerChars[i])  //string can't be right
                {
                    return false;
                }
            } else { //hasn't been solved
                if (used.contains(String.valueOf(inputChars[i]))) //string can't be right
                {
                    return false;
                }
            }
        }
        return true;
    }

}