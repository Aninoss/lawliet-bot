package commands.runnables.casinocategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoAbstract;
import constants.LogStatus;
import core.EmbedFactory;
import core.FileManager;
import core.LocalFile;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;

import java.io.IOException;
import java.util.*;

@CommandProperties(
        trigger = "hangman",
        emoji = "\uD83D\uDD21",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"hm"}
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
    public boolean onGameStart(CommandEvent event, String args) throws IOException {
        Random r = new Random();
        List<String> wordList = FileManager.readInList(new LocalFile(LocalFile.Directory.RESOURCES, "hangman_" + getLocale().getDisplayName() + ".txt"));
        answer = wordList.get(r.nextInt(wordList.size()));
        progress = new boolean[answer.length()];
        return true;
    }

    @Override
    public boolean onButtonCasino(ButtonInteractionEvent event) {
        String id = "text";
        TextInput textInput = TextInput.create(id, TextInputStyle.SHORT)
                .setRequiredRange(1, 100)
                .build();

        Modal modal = ModalMediator.createDrawableCommandModal(this, getString("guessletter"), e -> {
                    String input = e.getValue(id).getAsString().toUpperCase();

                    if (input.length() != 1) {
                        used.add(input);
                        if (answer.equals(input)) {
                            Arrays.fill(progress, true);
                            onRight(event.getMember(), input);
                        } else {
                            onWrong(event.getMember(), input);
                        }
                        return null;
                    }

                    char inputChar = input.charAt(0);
                    if (!Character.isLetter(inputChar)) {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
                        return null;
                    }
                    if (used.contains(String.valueOf(inputChar))) {
                        setLog(LogStatus.FAILURE, getString("used", input));
                        return null;
                    }

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
                    return null;
                })
                .addComponents(Label.of(getString("letterorword"), textInput))
                .build();

        event.replyModal(modal).queue();
        return false;
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
                StringUtil.escapeMarkdown(playerName),
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
        if (getStatus() == Status.ACTIVE) {
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, "0", getString("guessletter")),
                    BUTTON_CANCEL
            );
        }
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

    private void onWrong(Member member, String input) {
        health--;
        wrongAnswer = true;

        if (health > 0) {
            setLog(LogStatus.FAILURE, getString("wrong", input));
        } else {
            lose(member, true);
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

}