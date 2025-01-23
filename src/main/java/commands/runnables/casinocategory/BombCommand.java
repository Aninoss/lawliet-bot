package commands.runnables.casinocategory;

import commands.listeners.CommandProperties;
import commands.runnables.CasinoMultiplayerAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.atomicassets.AtomicMember;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CommandProperties(
        trigger = "bomb",
        releaseDate = {2024, 6, 7},
        emoji = "✂️",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        usesExtEmotes = true
)
public class BombCommand extends CasinoMultiplayerAbstract {

    private static final String[] COLOR_EMOJIS = new String[]{"🟥", "🟦", "🟩", "🟨"};

    private enum PlayerStatus {NONE, PENDING, RIGHT}

    private boolean active = false;
    private int currentPlayer = -1;
    private int currentColor = -1;
    private boolean[] alive;
    private PlayerStatus playerStatus = PlayerStatus.NONE;
    private ArrayList<Integer> availableColors = new ArrayList<>();
    private ScheduledFuture<?> timeOutFuture;

    public BombCommand(Locale locale, String prefix) {
        super(locale, prefix, 2, 8, false);
    }

    @Override
    public void onGameStart(List<AtomicMember> players) {
        currentPlayer = ThreadLocalRandom.current().nextInt(players.size());
        alive = new boolean[players.size()];
        Arrays.fill(alive, true);
        refillColors();

        setLog(null, getString("start", players.get(currentPlayer).getName(getLocale())));
        schedule(Duration.ofSeconds(3), this::startTurn);
    }

    @Override
    public synchronized boolean onButtonCasino(ButtonInteractionEvent event, int player) {
        if (player != currentPlayer || !active) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString("notyourturn")).build())
                    .setEphemeral(true)
                    .queue();
            return false;
        }

        pickColor(Integer.parseInt(event.getComponentId()));
        return true;
    }

    @Override
    public EmbedBuilder drawCasino() {
        List<AtomicMember> players = getPlayerList();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            sb.append(i == currentPlayer ? "➡️" : Emojis.FULL_SPACE_EMOTE.getFormatted())
                    .append(" ｜ ")
                    .append(getPlayerStatusEmoji(i))
                    .append(" ");
            if (!alive[i]) {
                sb.append("~~");
            }
            sb.append(players.get(i).getName(getLocale()));
            if (!alive[i]) {
                sb.append("~~");
            }
            sb.append("\n");
        }

        List<Button> buttons = availableColors.stream()
                .map(i -> {
                    Button button = Button.of(i == currentColor ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY, String.valueOf(i), Emoji.fromUnicode(COLOR_EMOJIS[i]));
                    if (!active) {
                        button = button.asDisabled();
                    }
                    return button;
                })
                .collect(Collectors.toList());
        setComponents(buttons);

        return EmbedFactory.getEmbedDefault(this, sb.toString());
    }

    private void refillColors() {
        availableColors = IntStream.range(0, COLOR_EMOJIS.length).boxed().collect(Collectors.toCollection(ArrayList::new));
    }

    private void startTurn() {
        active = true;
        currentColor = -1;
        playerStatus = PlayerStatus.NONE;

        setLog(null, getString(availableColors.size() == COLOR_EMOJIS.length ? "select_new" : "select", getPlayerList().get(currentPlayer).getName(getLocale())));
        redraw();

        timeOutFuture = schedule(Duration.ofSeconds(10), () -> {
            int color = availableColors.get(ThreadLocalRandom.current().nextInt(availableColors.size()));
            pickColor(color);
            redraw();
        });
    }

    private synchronized void pickColor(int color) {
        if (!active) {
            return;
        }

        active = false;
        currentColor = color;
        playerStatus = PlayerStatus.PENDING;
        timeOutFuture.cancel(false);
        setLog(null, getString("wait_" + ThreadLocalRandom.current().nextInt(7)));

        schedule(Duration.ofSeconds(5), this::revealResult);
    }

    private void revealResult() {
        boolean right = ThreadLocalRandom.current().nextInt(availableColors.size()) > 0;
        if (right) {
            setLog(LogStatus.SUCCESS, getString("right"));
            playerStatus = PlayerStatus.RIGHT;
        } else {
            setLog(LogStatus.FAILURE, getString("wrong"));
            alive[currentPlayer] = false;
        }
        redraw();

        schedule(Duration.ofSeconds(3), () -> {
            int onlyAlivePlayer = getOnlyAlivePlayer();
            if (onlyAlivePlayer != -1) {
                end(List.of(onlyAlivePlayer));
                availableColors.clear();
                redraw();
                return;
            }

            do {
                currentPlayer = (currentPlayer + 1) % getPlayerList().size();
            } while (!alive[currentPlayer]);

            availableColors.remove(Integer.valueOf(currentColor));
            if (!right || availableColors.size() <= 1) {
                refillColors();
            }
            startTurn();
        });
    }

    private int getOnlyAlivePlayer() {
        int onlyAlivePlayer = -1;
        for (int i = 0; i < alive.length; i++) {
            if (!alive[i]) {
                continue;
            }
            if (onlyAlivePlayer == -1) {
                onlyAlivePlayer = i;
            } else {
                onlyAlivePlayer = -1;
                break;
            }
        }
        return onlyAlivePlayer;
    }

    private String getPlayerStatusEmoji(int player) {
        if (player == currentPlayer && alive[player]) {
            if (active && currentColor == -1) {
                return Emojis.COUNTDOWN_10.getFormatted();
            } else {
                return switch (playerStatus) {
                    case NONE -> Emojis.FULL_SPACE_EMOTE.getFormatted();
                    case PENDING -> Emojis.LOADING.getFormatted();
                    case RIGHT -> "✅";
                };
            }
        } else {
            return alive[player] ? Emojis.FULL_SPACE_EMOTE.getFormatted() : "💥";
        }
    }

}