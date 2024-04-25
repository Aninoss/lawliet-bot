package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.EmojiStateProcessor;
import commands.stateprocessor.FileStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.LocalFile;
import core.TextManager;
import core.atomicassets.AtomicGuildMessageChannel;
import core.modals.DurationModalBuilder;
import core.modals.IntModalBuilder;
import core.modals.StringModalBuilder;
import core.utils.*;
import modules.Giveaway;
import modules.schedulers.GiveawayScheduler;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.giveaway.DBGiveaway;
import mysql.modules.giveaway.GiveawayData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "giveaway",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🎆",
        releaseDate = {2020, 10, 28},
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"giveaways"}
)
public class GiveawayCommand extends NavigationAbstract implements OnReactionListener {

    public final static int ITEM_LENGTH_MAX = MessageEmbed.TITLE_MAX_LENGTH;
    public final static int DESC_LENGTH_MAX = MessageEmbed.VALUE_MAX_LENGTH;
    public final static int WINNERS_MIN = 1;
    public final static int WINNERS_MAX = 20;

    private final static int
            STATE_SET_CHANNEL = 1,
            STATE_EDIT_SELECT_GIVEAWAY = 2,
            STATE_REROLL_SELECT_GIVEAWAY = 3,
            STATE_CONFIG = 4,
            STATE_SET_DESC = 5,
            STATE_SET_EMOJI = 6,
            STATE_SET_IMAGE = 7,
            STATE_EXAMPLE = 8,
            STATE_REROLL = 9;

    private CustomObservableMap<Long, GiveawayData> giveawayMap = null;

    private long messageId;
    private String item = "";
    private String previousItem = "";
    private String description = "";
    private long durationMinutes = 10080;
    private int amountOfWinners = 1;
    private Emoji emoji = Emoji.fromUnicode("🎉");
    private String imageUrl;
    private LocalFile imageCdn;
    private AtomicGuildMessageChannel channel;
    private Instant instant;
    private boolean editMode = false;
    private GiveawayData rerollGiveawayData;
    private int rerollWinners;
    private EmojiStateProcessor emojiStateProcessor;

    public GiveawayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        giveawayMap = DBGiveaway.getInstance().retrieve(event.getGuild().getIdLong());
        emojiStateProcessor = new EmojiStateProcessor(this, STATE_SET_EMOJI, STATE_CONFIG, getString("state3_memoji"))
                .setClearButton(false)
                .setGetter(() -> this.emoji)
                .setSetter(emoji -> this.emoji = emoji);

        registerNavigationListener(event.getMember(), List.of(
                new StringStateProcessor(this, STATE_SET_DESC, STATE_CONFIG, getString("state3_mdescription"))
                        .setClearButton(true)
                        .setMax(DESC_LENGTH_MAX)
                        .setGetter(() -> description)
                        .setSetter(value -> description = value != null ? value : ""),
                new FileStateProcessor(this, STATE_SET_IMAGE, STATE_CONFIG, getString("dashboard_includedimage"))
                        .setClearButton(true)
                        .setAllowGifs(true)
                        .setGetter(() -> imageUrl)
                        .setSetter(attachment -> {
                            if (attachment != null) {
                                LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("giveaway/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
                                FileUtil.downloadImageAttachment(attachment, tempFile);
                                imageUrl = uploadFile(tempFile);
                            } else {
                                deleteTemporaryImage();
                                imageUrl = null;
                            }
                        }),
                emojiStateProcessor
        ));
        registerReactionListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonAddOrEdit(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deleteTemporaryImage();
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                setState(STATE_SET_CHANNEL);
                editMode = false;
                return true;

            case 1:
                if (!getActiveGiveawaySlots().isEmpty()) {
                    setState(STATE_EDIT_SELECT_GIVEAWAY);
                    editMode = true;
                } else {
                    setLog(LogStatus.FAILURE, getString("nothing"));
                }
                return true;

            case 2:
                if (!getCompletedGiveawaySlots().isEmpty()) {
                    setState(STATE_REROLL_SELECT_GIVEAWAY);
                } else {
                    setLog(LogStatus.FAILURE, getString("nothing_completed"));
                }
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_CHANNEL)
    public boolean onButtonAddMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_EDIT_SELECT_GIVEAWAY)
    public boolean onButtonEditMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        List<GiveawayData> giveaways = getActiveGiveawaySlots();
        if (i >= 0 && i < giveaways.size()) {
            GiveawayData giveaway = giveaways.get(i);
            messageId = giveaway.getMessageId();
            item = giveaway.getTitle();
            previousItem = item;
            description = giveaway.getDescription();
            durationMinutes = giveaway.getDurationMinutes();
            amountOfWinners = giveaway.getWinners();
            imageUrl = giveaway.getImageUrl().orElse(null);
            channel = new AtomicGuildMessageChannel(event.getGuild().getIdLong(), giveaway.getGuildMessageChannelId());
            instant = giveaway.getStart();
            emoji = Emoji.fromFormatted(giveaway.getEmoji());
            deleteTemporaryImage();
            setState(STATE_CONFIG);

            return true;
        }

        return false;
    }

    @ControllerButton(state = STATE_REROLL_SELECT_GIVEAWAY)
    public boolean onButtonRerollMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        List<GiveawayData> giveaways = getCompletedGiveawaySlots();
        if (i >= 0 && i < giveaways.size()) {
            rerollGiveawayData = giveaways.get(i);
            rerollWinners = rerollGiveawayData.getWinners();
            setState(STATE_REROLL);
            return true;
        }

        return false;
    }

    @ControllerButton(state = STATE_CONFIG)
    public boolean onButtonConfigureMessage(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                if (!editMode) {
                    setState(STATE_SET_CHANNEL);
                } else {
                    resetGiveawayConfiguration();
                    deleteTemporaryImage();
                    setState(STATE_EDIT_SELECT_GIVEAWAY);
                }
                return true;

            case 0:
                Modal modal = new StringModalBuilder(this, getString("state3_mtitle"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, ITEM_LENGTH_MAX)
                        .setGetter(() -> item)
                        .setSetter(value -> item = value)
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                setState(STATE_SET_DESC);
                return true;

            case 2:
                if (!editMode) {
                    modal = new DurationModalBuilder(this, getString("state3_mduration"))
                            .setMinMinutes(1)
                            .setGetter(() -> durationMinutes)
                            .setSetter(value -> durationMinutes = value)
                            .build();
                    event.replyModal(modal).queue();
                    return false;
                } else {
                    setLog(LogStatus.FAILURE, getString("locked"));
                    return true;
                }

            case 3:
                modal = new IntModalBuilder(this, getString("state3_mwinners"))
                        .setMinMax(1, WINNERS_MAX)
                        .setGetter(() -> amountOfWinners)
                        .setSetter(value -> amountOfWinners = value)
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 4:
                if (!editMode) {
                    setState(STATE_SET_EMOJI);
                } else {
                    setLog(LogStatus.FAILURE, getString("locked"));
                }
                return true;

            case 5:
                setState(STATE_SET_IMAGE);
                return true;

            case 6:
                if (item.isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("noitem"));
                    return true;
                }

                setState(STATE_EXAMPLE);
                return true;

            case 7:
                if (item.isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("noitem"));
                    return true;
                }

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.GIVEAWAYS_END, event.getMember(), previousItem);
                getEntityManager().getTransaction().commit();

                send(event, true);
                return true;

            case 8:
                if (item.isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("noitem"));
                    return true;
                }

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), editMode ? BotLogEntity.Event.GIVEAWAYS_EDIT : BotLogEntity.Event.GIVEAWAYS_ADD, event.getMember(), previousItem);
                getEntityManager().getTransaction().commit();

                send(event, false);
                return true;

            default:
                return false;
        }
    }

    private void resetGiveawayConfiguration() {
        messageId = 0L;
        item = "";
        previousItem = item;
        description = "";
        durationMinutes = 10080;
        amountOfWinners = 1;
        imageUrl = null;
        channel = null;
        instant = null;
        emoji = Emoji.fromUnicode("🎉");
    }

    private void send(ButtonInteractionEvent event, boolean endPrematurely) {
        if (editMode && (!giveawayMap.containsKey(messageId) || !giveawayMap.get(messageId).isActive())) {
            setLog(LogStatus.FAILURE, getString("dashboard_toolate"));
            setState(STATE_EDIT_SELECT_GIVEAWAY);
        }

        Optional<Long> messageIdOpt = sendMessage();
        if (messageIdOpt.isEmpty() && editMode) {
            setLog(LogStatus.FAILURE, getString("nomessage"));
            setState(STATE_EDIT_SELECT_GIVEAWAY);
            return;
        }

        if (messageIdOpt.isPresent()) {
            setLog(LogStatus.SUCCESS, getString("sent", item));
            setState(editMode && (!endPrematurely || giveawayMap.size() > 1) ? STATE_EDIT_SELECT_GIVEAWAY : DEFAULT_STATE);

            GiveawayData giveawayData = new GiveawayData(
                    event.getGuild().getIdLong(),
                    channel.getIdLong(),
                    messageIdOpt.get(),
                    emoji.getFormatted(),
                    amountOfWinners,
                    instant,
                    endPrematurely ? 0 : durationMinutes,
                    item,
                    description,
                    imageUrl,
                    true
            );
            if (endPrematurely || !giveawayMap.containsKey(giveawayData.getMessageId())) {
                GiveawayScheduler.loadGiveawayBean(giveawayData);
            }
            giveawayMap.put(giveawayData.getMessageId(), giveawayData);
            imageCdn = null;
            resetGiveawayConfiguration();
        } else {
            setLog(LogStatus.FAILURE, getString("error"));
        }
    }

    @Override
    public boolean onReaction(@NotNull GenericMessageReactionEvent event) {
        return emojiStateProcessor.handleReactionEvent(event);
    }

    @ControllerButton(state = STATE_REROLL)
    public boolean onButtonRerollNumber(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(STATE_REROLL_SELECT_GIVEAWAY);
                return true;
            }
            case 0 -> {
                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.GIVEAWAYS_REMOVE, event.getMember(), rerollGiveawayData.getTitle());
                getEntityManager().getTransaction().commit();

                giveawayMap.remove(rerollGiveawayData.getMessageId());
                setLog(LogStatus.SUCCESS, getString("removed", rerollGiveawayData.getTitle()));
                setState(giveawayMap.isEmpty() ? DEFAULT_STATE : STATE_REROLL_SELECT_GIVEAWAY);
                return true;
            }
            case 1 -> {
                Modal modal = new IntModalBuilder(this, getString("state3_mwinners"))
                        .setMinMax(WINNERS_MIN, WINNERS_MAX)
                        .setGetter(() -> rerollWinners)
                        .setSetter(value -> rerollWinners = value)
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                boolean messageExists = GiveawayScheduler.processGiveawayUsers(rerollGiveawayData, rerollWinners, true).join();
                getEntityManager().getTransaction().begin();
                if (messageExists) {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.GIVEAWAYS_REROLL, event.getMember(), rerollGiveawayData.getTitle());
                    setLog(LogStatus.SUCCESS, getString("rerollset", rerollGiveawayData.getTitle()));
                } else {
                    setLog(LogStatus.FAILURE, getString("error"));
                }
                getEntityManager().getTransaction().commit();
                setState(STATE_REROLL_SELECT_GIVEAWAY);
                return true;
            }
        }
        return false;
    }

    @ControllerButton
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(STATE_CONFIG);
            return true;
        }
        return false;
    }

    @ControllerEntitySelectMenu(state = STATE_SET_CHANNEL)
    public boolean onSelectMenuAddMessage(EntitySelectInteractionEvent event) {
        GuildMessageChannel channel = (GuildMessageChannel) event.getMentions().getChannels().get(0);
        if (checkWriteEmbedInChannelWithLog(channel)) {
            this.channel = new AtomicGuildMessageChannel(channel);
            setState(STATE_CONFIG);
        }
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawAddOrEdit(Member member) {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = STATE_SET_CHANNEL)
    public EmbedBuilder onDrawAddMessage(Member member) {
        EntitySelectMenu channelSelectMenu = EntitySelectMenu.create("select_channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                .setRequiredRange(1, 1)
                .build();
        setComponents(channelSelectMenu);
        return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));
    }

    @Draw(state = STATE_EDIT_SELECT_GIVEAWAY)
    public EmbedBuilder onDrawEditMessage(Member member) {
        String[] options = getActiveGiveawaySlots().stream()
                .map(giveawayData -> {
                    AtomicGuildMessageChannel atomicGuildMessageChannel = new AtomicGuildMessageChannel(giveawayData.getGuildId(), giveawayData.getGuildMessageChannelId());
                    return getString("state2_slot", giveawayData.getTitle(), atomicGuildMessageChannel.getName(getLocale()));
                })
                .toArray(String[]::new);
        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_REROLL_SELECT_GIVEAWAY)
    public EmbedBuilder onDrawRerollMessage(Member member) {
        String[] options = getCompletedGiveawaySlots().stream()
                .map(giveawayData -> getString("state2_slot", giveawayData.getTitle(), new AtomicGuildMessageChannel(member.getGuild().getIdLong(), giveawayData.getGuildMessageChannelId()).getName(getLocale())))
                .toArray(String[]::new);
        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state12_description"), getString("state12_title"));
    }

    @Draw(state = STATE_CONFIG)
    public EmbedBuilder onDrawConfigureMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        String[] options = getString("state3_options").split("\n");
        if (!editMode) {
            options[7] = "";
        }
        setComponents(options, Set.of(7, 8));

        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title_" + (editMode ? "edit" : "new")))
                .addField(getString("state3_mtitle"), StringUtil.escapeMarkdown(item.isEmpty() ? notSet : item), false)
                .addField(getString("state3_mdescription"), StringUtil.escapeMarkdown(description.isEmpty() ? notSet : description), false)
                .addField(getString("state3_mduration"), TimeUtil.getDurationString(getLocale(), Duration.ofMinutes(durationMinutes)), true)
                .addField(getString("state3_mwinners"), String.valueOf(amountOfWinners), true)
                .addField(getString("state3_memoji"), emoji.getFormatted(), true)
                .addField(getString("state3_mimage"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), imageUrl != null), true);
    }

    @Draw(state = STATE_EXAMPLE)
    public EmbedBuilder onDrawExample(Member member) {
        return Giveaway.getMessageEmbed(getLocale(), item, description, amountOfWinners, emoji,
                durationMinutes, imageUrl, Instant.now()
        );
    }

    @Draw(state = STATE_REROLL)
    public EmbedBuilder onDrawRerollNumber(Member member) {
        setComponents(getString("state13_options").split("\n"), Set.of(2));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state13_description", rerollGiveawayData.getTitle(), StringUtil.numToString(rerollWinners)),
                getString("state13_title")
        );
    }

    private String uploadFile(LocalFile file) {
        deleteTemporaryImage();
        imageCdn = file;
        return file.cdnGetUrl();
    }

    private void deleteTemporaryImage() {
        if (imageCdn != null) {
            imageCdn.delete();
            imageCdn = null;
        }
    }

    private List<GiveawayData> getActiveGiveawaySlots() {
        return giveawayMap.values().stream()
                .filter(GiveawayData::isActive)
                .collect(Collectors.toList());
    }

    private List<GiveawayData> getCompletedGiveawaySlots() {
        return giveawayMap.values().stream()
                .filter(g -> !g.isActive())
                .collect(Collectors.toList());
    }

    private Optional<Long> sendMessage() {
        Message message;
        if (checkWriteEmbedInChannelWithLog(channel.get().orElse(null))) {
            GuildMessageChannel channel = this.channel.get().get();
            if (!editMode) {
                instant = Instant.now();
                EmbedBuilder eb = Giveaway.getMessageEmbed(getLocale(), item, description, amountOfWinners, emoji,
                        durationMinutes, imageUrl, instant
                );
                message = channel.sendMessageEmbeds(eb.build()).complete();
                if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_HISTORY)) {
                    message.addReaction(emoji).queue();
                }
                return Optional.of(message.getIdLong());
            } else {
                if (instant.plus(durationMinutes, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                    return Optional.empty();
                }
                EmbedBuilder eb = Giveaway.getMessageEmbed(getLocale(), item, description, amountOfWinners, emoji,
                        durationMinutes, imageUrl, instant
                );
                try {
                    channel.editMessageEmbedsById(messageId, eb.build()).complete();
                    return Optional.of(messageId);
                } catch (Throwable e) {
                    //Ignore
                }

                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

}
