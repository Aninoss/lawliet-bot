package commands.runnables;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.CommandListenerMeta;
import commands.listeners.OnButtonListener;
import commands.listeners.OnStringSelectMenuListener;
import constants.Emojis;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicMember;
import core.interactionresponse.InteractionResponse;
import core.mention.Mention;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import kotlin.jvm.functions.Function1;
import modules.fishery.FisheryPowerUp;
import modules.fishery.FisheryStatus;
import mysql.modules.casinostats.DBCasinoStats;
import mysql.modules.casinotracking.DBCasinoTracking;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class CasinoMultiplayerAbstract extends Command implements OnButtonListener, OnStringSelectMenuListener {

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
    private final boolean enableDms;
    private final ArrayList<AtomicMember> playerList = new ArrayList<>();
    private final HashMap<Long, Boolean> trackingActiveMap = new HashMap<>();
    private final HashMap<Long, Long> userDmMessageMap = new HashMap<>();

    public CasinoMultiplayerAbstract(Locale locale, String prefix, long playersMin, long playersMax, boolean enableDms) {
        super(locale, prefix);
        this.playersMin = playersMin;
        this.playersMax = playersMax;
        this.enableDms = enableDms;
    }

    public boolean onGamePrepare(CommandEvent event, String args) throws Throwable {
        return true;
    }

    public abstract void onGameStart(List<AtomicMember> players) throws Throwable;

    public abstract boolean onButtonCasino(ButtonInteractionEvent event, int player) throws Throwable;

    public boolean onStringSelectMenuCasino(StringSelectInteractionEvent event, int player) throws Throwable {
        return false;
    }

    public abstract EmbedBuilder drawCasino() throws Throwable;

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (!onGamePrepare(event, args)) {
            return false;
        }
        if (enableDms && !sendDm(event.getUser())) {
            return false;
        }

        if (getGuildEntity().getFishery().getFisheryStatus() != FisheryStatus.ACTIVE) {
            coinsInput = 0;
            startLobby(event.getMember());
            return true;
        }

        fisheryGuildData = FisheryUserManager.getGuildData(event.getGuild().getIdLong());
        FisheryMemberData memberBean = FisheryUserManager.getGuildData(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
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
        Function1<? super GenericComponentInteractionCreateEvent, ? extends CommandListenerMeta.CheckResponse> validityFunction = event -> {
            if (event.getMessageIdLong() == getDrawMessageId().orElse(0L) || (enableDms && userDmMessageMap.values().stream().anyMatch(messageId -> messageId == event.getMessageIdLong()))) {
                return status == Status.WAITING_FOR_PLAYERS || playerList.stream().anyMatch(m -> m.getIdLong() == event.getUser().getIdLong())
                        ? CommandListenerMeta.CheckResponse.ACCEPT
                        : CommandListenerMeta.CheckResponse.DENY_WITHOUT_AUTHOR_MENTION;
            } else {
                return CommandListenerMeta.CheckResponse.IGNORE;
            }
        };

        registerButtonListener(member, true, validityFunction);
        registerStringSelectMenuListener(member, false, validityFunction);
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
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
                    if (playerList.get(i).getIdLong() == event.getUser().getIdLong()) {
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

    @Override
    public boolean onStringSelectMenu(@NotNull StringSelectInteractionEvent event) throws Throwable {
        if (status == Status.PLAYING) {
            int player = -1;
            for (int i = 0; i < playerList.size(); i++) {
                if (playerList.get(i).getIdLong() == event.getUser().getIdLong()) {
                    player = i;
                    break;
                }
            }
            return onStringSelectMenuCasino(event, player);
        } else {
            return false;
        }
    }

    private boolean onButtonJoin(ButtonInteractionEvent event) {
        if (playerList.stream().anyMatch(m -> m.getIdLong() == event.getMember().getIdLong())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_already_joined"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        }

        if (playerList.size() >= playersMax) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_toomany"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        }

        if (fisheryGuildData == null) {
            if (enableDms && !sendDm(event.getUser())) {
                return false;
            }
            playerList.add(new AtomicMember(event.getMember()));
            setLog(null, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_log", StringUtil.escapeMarkdownInField(event.getMember().getEffectiveName())));
            return true;
        }

        FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(event.getMember().getIdLong());
        if (fisheryMemberData.getCoins() >= coinsInput) {
            if (enableDms && !sendDm(event.getUser())) {
                return false;
            }
            fisheryMemberData.addCoinsHidden(coinsInput);
            playerList.add(new AtomicMember(event.getMember()));
            setLog(null, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_log", StringUtil.escapeMarkdownInField(event.getMember().getEffectiveName())));
            return true;
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_notenough"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        }
    }

    private boolean sendDm(User user) {
        try {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_dm_waiting", Emojis.LOADING.getFormatted()));
            Message message = JDAUtil.openPrivateChannel(user)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .complete();
            userDmMessageMap.put(user.getIdLong(), message.getIdLong());
        } catch (Throwable e) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_join_nodms"));
            InteractionResponse interactionResponse = getInteractionResponse();
            if (interactionResponse != null) {
                interactionResponse.replyEmbeds(List.of(eb.build()), true).queue();
            } else {
                drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            }
            return false;
        }
        return true;
    }

    private boolean onButtonLeave(ButtonInteractionEvent event) {
        if (playerList.stream().noneMatch(m -> m.getIdLong() == event.getMember().getIdLong())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_notyet_joined"));
            getInteractionResponse().replyEmbeds(List.of(eb.build()), true).queue();
            return false;
        } else {
            if (fisheryGuildData != null) {
                fisheryGuildData.getMemberData(event.getMember().getIdLong()).addCoinsHidden(-coinsInput);
            }
            playerList.removeIf(m -> m == null || m.getIdLong() == event.getMember().getIdLong());
            if (!playerList.isEmpty()) {
                setLog(null, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_leave_log", StringUtil.escapeMarkdownInField(event.getMember().getEffectiveName())));
                return true;
            } else {
                deregisterListenersWithComponentMessage();
                return false;
            }
        }
    }

    private synchronized boolean onButtonStart(ButtonInteractionEvent event) throws Throwable {
        if (playerList.size() >= playersMin) {
            if (event.getUser().getIdLong() == playerList.get(0).getIdLong()) {
                if (enableDms) {
                    for (AtomicMember atomicMember : playerList) {
                        JDAUtil.openPrivateChannel(event.getJDA(), atomicMember.getIdLong())
                                .flatMap(channel -> channel.sendMessage(atomicMember.getAsMention(getLocale())))
                                .flatMap(Message::delete)
                                .queue();
                    }
                } else {
                    if (playerList.size() > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < playerList.size(); i++) {
                            AtomicMember player = playerList.get(i);
                            sb.append(player.getAsMention(getLocale()));
                        }
                        drawMessageNew(sb.toString())
                                .thenAccept(m -> m.delete().queue())
                                .exceptionally(ExceptionLogger.get());
                    }
                }

                onGameStart(getPlayerList());
                playerList.forEach(atomicMember -> {
                    trackingActiveMap.put(atomicMember.getIdLong(),
                            coinsInput > 0 && DBCasinoTracking.getInstance().retrieve().isActive(atomicMember.getIdLong())
                    );
                });
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
        if (status != Status.PLAYING) {
            return;
        }
        deregisterListeners();

        long totalCoinsInput = coinsInput * playerList.size();
        long price = totalCoinsInput / winners.size();
        ArrayList<Member> winnersMembers = new ArrayList<>();
        ArrayList<Member> shieldProtectedMembers = new ArrayList<>();
        for (int player = 0; player < playerList.size(); player++) {
            AtomicMember atomicMember = playerList.get(player);
            if (fisheryGuildData != null) {
                FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(atomicMember.getIdLong());
                fisheryMemberData.addCoinsHidden(-coinsInput);
                if (winners.contains(player)) {
                    fisheryMemberData.addCoinsRaw(price - coinsInput);
                    atomicMember.get().ifPresent(winnersMembers::add);
                    if (trackingActiveMap.get(atomicMember.getIdLong())) {
                        DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(fisheryGuildData.getGuildId(), atomicMember.getIdLong()))
                                .add(getTrigger(), true, price - coinsInput);
                    }
                } else {
                    long coinsLost = coinsInput;
                    if (fisheryMemberData.getActivePowerUps().contains(FisheryPowerUp.SHIELD)) {
                        fisheryMemberData.deletePowerUp(FisheryPowerUp.SHIELD);
                        coinsLost = 0;
                        atomicMember.get().ifPresent(shieldProtectedMembers::add);
                    } else {
                        fisheryMemberData.addCoinsRaw(-coinsInput);
                    }
                    if (trackingActiveMap.get(atomicMember.getIdLong())) {
                        DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(fisheryGuildData.getGuildId(), atomicMember.getIdLong()))
                                .add(getTrigger(), false, coinsLost);
                    }
                }
            } else if (winners.contains(player)) {
                atomicMember.get().ifPresent(winnersMembers::add);
            }
        }

        if (!shieldProtectedMembers.isEmpty()) {
            Mention mentionedMembers = MentionUtil.getMentionedStringOfMembers(getLocale(), shieldProtectedMembers);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setDescription(TextManager.getString(getLocale(), Category.CASINO, "casino_protection", mentionedMembers.isMultiple(), mentionedMembers.getMentionText()))
                    .setThumbnail("https://cdn.discordapp.com/attachments/1077245845440827562/1080855203026313276/shield_break.gif");
            setAdditionalEmbeds(eb.build());
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
    public EmbedBuilder draw(Member member) throws Throwable {
        if (status == Status.WAITING_FOR_PLAYERS) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                    .addField(TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_players", StringUtil.numToString(playersMin)), generatePlayersList(), false)
                    .addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_template", "", StringUtil.numToString(coinsInput)), false);

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
            EmbedBuilder eb = drawCasino();
            if (getLog() != null) {
                EmbedUtil.addLog(eb, getLogStatus(), getLog());
                setLog(null, null);
            }
            if (enableDms) {
                for (int i = 0; i < playerList.size(); i++) {
                    AtomicMember atomicMember = playerList.get(i);
                    int finalI = i;
                    JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), atomicMember.getIdLong())
                            .flatMap(channel -> channel.editMessageEmbedsById(userDmMessageMap.get(atomicMember.getIdLong()), eb.build())
                                    .setComponents(status == Status.PLAYING ? generatePlayerActionRows(finalI) : Collections.emptyList()))
                            .queue();
                }
                if (status == Status.PLAYING) {
                    EmbedUtil.addLog(eb, LogStatus.WARNING, TextManager.getString(getLocale(), Category.CASINO, "casino_multiplayer_dm_log"));
                }
            }
            return eb;
        }
    }

    public void redraw() {
        try {
            drawMessage(draw(null)).exceptionally(ExceptionLogger.get());
        } catch (Throwable e) {
            MainLogger.get().error("Exception", e);
        }
    }

    public Collection<? extends LayoutComponent> generatePlayerActionRows(int player) {
        return Collections.emptyList();
    }

    private String generatePlayersList() {
        String notSet = Emojis.LOADING.getFormatted();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < playersMax; i++) {
            String playerTag = playerList.size() > i ? StringUtil.escapeMarkdown(playerList.get(i).getName(getLocale())) : notSet;
            sb.append(i + 1).append(" - ");
            if (i == 0 && !playerList.isEmpty()) {
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

    public Status getStatus() {
        return status;
    }

}