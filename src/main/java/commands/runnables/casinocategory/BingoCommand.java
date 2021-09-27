package commands.runnables.casinocategory;

import java.util.*;
import java.util.stream.Collectors;
import commands.Category;
import commands.listeners.CommandProperties;
import commands.runnables.CasinoMultiplayerAbstract;
import constants.Emojis;
import core.EmbedFactory;
import core.MainLogger;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.schedule.MainScheduler;
import core.utils.StringUtil;
import modules.BingoBoard;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "bingo",
        releaseDate = { 2021, 10, 5 },
        emoji = "🎱",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        executableWithoutArgs = true,
        usesExtEmotes = true
)
public class BingoCommand extends CasinoMultiplayerAbstract {

    private BingoBoard[] boards;
    private int[] boardToPlayer;
    private int[] playerToBoard;
    private boolean selectionMode = true;
    private List<Integer> balls;
    private int ballsDisclosed = 1;
    private boolean disclosurePending = true;

    public BingoCommand(Locale locale, String prefix) {
        super(locale, prefix, 2, 8);
    }

    @Override
    public void onGameStart(List<AtomicMember> players) throws Throwable {
        boards = new BingoBoard[players.size()];
        for (int i = 0; i < boards.length; i++) {
            boards[i] = new BingoBoard(i);
        }
        boardToPlayer = new int[boards.length];
        Arrays.fill(boardToPlayer, -1);
        playerToBoard = new int[boards.length];
        Arrays.fill(playerToBoard, -1);

        balls = new ArrayList<>();
        for (int i = 0; i < BingoBoard.EMOJIS.length; i++) {
            balls.add(i);
        }
        Collections.shuffle(balls);
    }

    @Override
    public synchronized boolean onButtonCasino(ButtonClickEvent event, int player) throws Throwable {
        int boardId = Integer.parseInt(event.getComponentId());
        if (boardToPlayer[boardId] != -1) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("boardused"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        }

        if (playerToBoard[player] == -1) {
            boardToPlayer[boardId] = player;
            playerToBoard[player] = boardId;
            List<Integer> remainingBoards = Arrays.stream(boards)
                    .map(BingoBoard::getId)
                    .filter(id -> boardToPlayer[id] == -1)
                    .collect(Collectors.toList());
            if (remainingBoards.size() == 1) {
                List<Integer> remainingPlayers = Arrays.stream(boards)
                        .map(BingoBoard::getId)
                        .filter(id -> playerToBoard[id] == -1)
                        .collect(Collectors.toList());

                int newBoardId = remainingBoards.get(0);
                int newPlayer = remainingPlayers.get(0);
                boardToPlayer[newBoardId] = newPlayer;
                playerToBoard[newPlayer] = newBoardId;

                deregisterListeners();
                selectionMode = false;
                startDisclosure(event.getMember());
            }
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("playeralreadyset"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        }
    }

    private void startDisclosure(Member member) {
        MainScheduler.schedule(3000, "Bingo-Disclosure", () -> disclosureStep(member));
    }

    private void disclosureStep(Member member) {
        if (disclosurePending) {
            disclosurePending = false;
            MainScheduler.schedule(2000, "Bingo-Disclosure", () -> disclosureStep(member));
        } else {
            ArrayList<Integer> winners = new ArrayList<>();
            for (int player = 0; player < playerToBoard.length; player++) {
                int boardId = playerToBoard[player];
                BingoBoard board = boards[boardId];
                board.solve(balls.get(ballsDisclosed - 1));
                if (board.completed()) {
                    winners.add(player);
                }
            }
            if (winners.size() > 0) {
                end(winners);
            } else {
                ballsDisclosed++;
                disclosurePending = true;
                MainScheduler.schedule(3000, "Bingo-Disclosure", () -> disclosureStep(member));
            }
        }
        try {
            drawMessage(draw(member));
        } catch (Throwable e) {
            MainLogger.get().error("Exception", e);
        }
    }

    @Override
    public EmbedBuilder drawCasino(Member member) {
        StringBuilder sb = new StringBuilder();
        if (selectionMode) {
            sb.append(getString("selectboard"))
                    .append("\n\n");

            List<AtomicMember> playerList = getPlayerList();
            for (int i = 0; i < playerList.size(); i++) {
                AtomicMember atomicMember = playerList.get(i);
                int boardId = playerToBoard[i];
                String playerBoardValue = boardId != -1
                        ? "**" + getString("board", StringUtil.numToString(boardId + 1)) + "**"
                        : TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
                sb.append(getString("memberboardselect", atomicMember.getAsMention(), playerBoardValue))
                        .append("\n");
            }
            sb.append("\n");
        } else {
            sb.append(getString("disclosed"))
                    .append(" ");
            for (int i = 0; i < ballsDisclosed; i++) {
                if (i == ballsDisclosed - 1 && disclosurePending) {
                    sb.append(Emojis.COUNTDOWN_3);
                } else {
                    sb.append('`')
                            .append(BingoBoard.EMOJIS[balls.get(i)])
                            .append('`');
                }
                sb.append(" ");
            }
            sb.append("\n");
        }
        sb.append(TextManager.getString(getLocale(), Category.CASINO, "casino_coinsInput", "", StringUtil.numToString(getCoinsInput())));

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, sb.toString());
        ArrayList<Button> buttons = new ArrayList<>();
        for (BingoBoard board : boards) {
            String boardName = selectionMode
                    ? getString("board", StringUtil.numToString(board.getId() + 1))
                    : getPlayerList().get(boardToPlayer[board.getId()]).getName();
            eb.addField(boardName, board.draw(), true);

            Button button = Button.of(ButtonStyle.PRIMARY, String.valueOf(board.getId()), boardName);
            if (boardToPlayer[board.getId()] != -1) {
                button = button.asDisabled();
            }
            buttons.add(button);
        }

        if (selectionMode) {
            setComponents(buttons);
        }
        return eb;
    }

}