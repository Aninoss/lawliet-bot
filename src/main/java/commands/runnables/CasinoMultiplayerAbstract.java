package commands.runnables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.CommandListenerMeta;
import commands.listeners.OnButtonListener;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.mention.Mention;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.FisheryStatus;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

public abstract class CasinoMultiplayerAbstract extends Command implements OnButtonListener {

    public enum Status { WAITING_FOR_PLAYERS, PLAYING, END }

    public static final String BUTTON_ID_JOIN = "join";
    public static final String BUTTON_ID_LEAVE = "leave";
    public static final String BUTTON_ID_START = "start";

    private static final String EMOJI_HOST = "👑";

    private FisheryGuildData fisheryGuildData;
    private long coinsInput;
    private Status status = Status.WAITING_FOR_PLAYERS;
    private final long playersMin;
    private final long playersMax;
    private final ArrayList<AtomicMember> playerList = new ArrayList<>();

    public CasinoMultiplayerAbstract(Locale locale, String prefix, long playersMin, long playersMax) {
        super(locale, prefix);
        this.playersMin = playersMin;
        this.playersMax = playersMax;
    }

    public boolean onGamePrepare(CommandEvent event, String args) throws Throwable {
        return true;
    }

    public abstract void onGameStart(List<AtomicMember> players) throws Throwable;

    public abstract boolean onButtonCasino(ButtonClickEvent event, int player) throws Throwable;

    public abstract EmbedBuilder drawCasino(Member member) throws Throwable;

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (!onGamePrepare(event, args)) {
            return false;
        }

        FisheryStatus status = DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).getFisheryStatus();
        if (status != FisheryStatus.ACTIVE) {
            coinsInput = 0;
            startLobby(event.getMember());
            return true;
        }

        fisheryGuildData = DBFishery.getInstance().retrieve(event.getGuild().getIdLong());
        FisheryMemberData memberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        long coins = memberBean.getCoins();
        long value = Math.min(MentionUtil.getAmountExt(args, coins), coins);
        if (value == -1) {
            coinsInput = (long) Math.ceil(coins * 0.1);
            memberBean.addCoinsHidden(coinsInput);
            startLobby(event.getMember());
            return true;
        }

        if (value >= 0) {
            coinsInput = value;
            memberBean.addCoinsHidden(coinsInput);
            startLobby(event.getMember());
            return true;
        } else {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "0")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    public void startLobby(Member member) {
        playerList.add(new AtomicMember(member));
        registerButtonListener(member, true, event -> {
            if (event.getMessageIdLong() == getDrawMessageId().orElse(0L)) {
                return status == Status.WAITING_FOR_PLAYERS || playerList.stream().anyMatch(m -> m.getIdLong() == event.getMember().getIdLong())
                        ? CommandListenerMeta.CheckResponse.ACCEPT
                        : CommandListenerMeta.CheckResponse.DENY_WITHOUT_AUTHOR_MENTION;
            } else {
                return CommandListenerMeta.CheckResponse.IGNORE;
            }
        });
    }

    @Override
    public boolean onButton(@NotNull ButtonClickEvent event) throws Throwable {
        switch (status) {
            case WAITING_FOR_PLAYERS -> {
                return switch (event.getComponentId()) {
                    case BUTTON_ID_JOIN -> onButtonJoin(event);
                    case BUTTON_ID_LEAVE -> onButtonLeave(event);
                    case BUTTON_ID_START -> onButtonStart(event);
                    default -> false;
                };
            }
            case PLAYING -> {
                int player = -1;
                for (int i = 0; i < playerList.size(); i++) {
                    if (playerList.get(i).getIdLong() == event.getMember().getIdLong()) {
                        player = i;
                        break;
                    }
                }
                return onButtonCasino(event, player);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean onButtonJoin(ButtonClickEvent event) {
        if (playerList.stream().anyMatch(m -> m.getIdLong() == event.getMember().getIdLong())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_already_joined"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        } else {
            if (playerList.size() >= playersMax) {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_toomany"));
                getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
                return false;
            }

            if (fisheryGuildData == null) {
                playerList.add(new AtomicMember(event.getMember()));
                setLog(null, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_log", event.getMember().getEffectiveName()));
                return true;
            }

            FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(event.getMember().getIdLong());
            if (fisheryMemberData.getCoins() >= coinsInput) {
                fisheryMemberData.addCoinsHidden(coinsInput);
                playerList.add(new AtomicMember(event.getMember()));
                setLog(null, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_log", event.getMember().getEffectiveName()));
                return true;
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_notenough"));
                getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
                return false;
            }
        }
    }

    private boolean onButtonLeave(ButtonClickEvent event) {
        if (playerList.stream().noneMatch(m -> m.getIdLong() == event.getMember().getIdLong())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_notyet_joined"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        } else {
            if (fisheryGuildData != null) {
                fisheryGuildData.getMemberData(event.getMember().getIdLong()).addCoinsHidden(-coinsInput);
            }
            playerList.removeIf(m -> m == null || m.getIdLong() == event.getMember().getIdLong());
            if (playerList.size() > 0) {
                setLog(null, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_leave_log", event.getMember().getEffectiveName()));
                return true;
            } else {
                deregisterListenersWithComponentMessage();
                return false;
            }
        }
    }

    private synchronized boolean onButtonStart(ButtonClickEvent event) throws Throwable {
        if (playerList.size() >= playersMin) {
            if (event.getMember().getIdLong() == playerList.get(0).getIdLong()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < playerList.size(); i++) {
                    AtomicMember player = playerList.get(i);
                    sb.append(player.getAsMention());
                }
                drawMessageNew(sb.toString())
                        .thenAccept(m -> m.delete().queue())
                        .exceptionally(ExceptionLogger.get());
                onGameStart(getPlayerList());
                status = Status.PLAYING;
                return true;
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_start_nothost"));
                getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
                return false;
            }
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_start_notenough"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        }
    }

    protected void end(List<Integer> winners) {
        deregisterListeners();

        long totalCoinsInput = coinsInput * playerList.size();
        long price = totalCoinsInput / winners.size();
        ArrayList<Member> winnersMembers = new ArrayList<>();
        for (int player = 0; player < playerList.size(); player++) {
            AtomicMember atomicMember = playerList.get(player);
            if (fisheryGuildData != null) {
                FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(atomicMember.getIdLong());
                fisheryMemberData.addCoinsHidden(-coinsInput);
                if (winners.contains(player)) {
                    fisheryMemberData.addCoinsRaw(price - coinsInput);
                    atomicMember.get().ifPresent(winnersMembers::add);
                } else {
                    fisheryMemberData.addCoinsRaw(-coinsInput);
                }
            } else if (winners.contains(player)) {
                atomicMember.get().ifPresent(winnersMembers::add);
            }
        }

        Mention mention = MentionUtil.getMentionedStringOfMembers(getLocale(), winnersMembers);
        String key = price != 1 ? "casino_multiplayer_win" : "casino_multiplayer_win_singlecoin";
        String text = TextManager.getString(getLocale(), Category.CASINO, key,
                mention.isMultiple(),
                mention.getMentionText().replace("**", ""),
                StringUtil.numToString(price)
        );
        setLog(LogStatus.WIN, text);
        status = Status.END;
    }

    @Override
    public EmbedBuilder draw(@NotNull Member member) throws Throwable {
        if (status == Status.WAITING_FOR_PLAYERS) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .addField(TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_players", StringUtil.numToString(playersMin)), generatePlayersList(), false)
                    .addField(Emojis.ZERO_WIDTH_SPACE, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_template", "", StringUtil.numToString(coinsInput)), false);

            Button startButton = Button.of(ButtonStyle.SUCCESS, BUTTON_ID_START, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_start"), Emoji.fromUnicode(EMOJI_HOST));
            if (playerList.size() < playersMin) {
                startButton = startButton.asDisabled();
            }
            setComponents(
                    Button.of(ButtonStyle.PRIMARY, BUTTON_ID_JOIN, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join")),
                    Button.of(ButtonStyle.DANGER, BUTTON_ID_LEAVE, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_leave")),
                    startButton
            );

            return eb;
        } else {
            return drawCasino(member);
        }
    }

    private String generatePlayersList() {
        String notSet = Emojis.LOADING;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < playersMax; i++) {
            String playerTag = playerList.size() > i ? playerList.get(i).getAsMention() : notSet;
            sb.append(i + 1).append(" - ");
            if (i == 0 && playerList.size() > 0) {
                sb.append(EMOJI_HOST).append(" ");
            }
            sb.append(playerTag).append("\n");
        }

        return sb.toString();
    }

    @Override
    public void onListenerTimeOut() {
        if (status == Status.WAITING_FOR_PLAYERS && fisheryGuildData != null) {
            for (AtomicMember atomicMember : playerList) {
                fisheryGuildData.getMemberData(atomicMember.getIdLong()).addCoinsHidden(-coinsInput);
            }
        }
    }

    public List<AtomicMember> getPlayerList() {
        return Collections.unmodifiableList(playerList);
    }

    public long getCoinsInput() {
        return coinsInput;
    }

}