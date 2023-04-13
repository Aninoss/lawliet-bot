package commands.runnables.utilitycategory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnReactionListener;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicStandardGuildMessageChannel;
import core.atomicassets.MentionableAtomicAsset;
import core.utils.*;
import modules.Giveaway;
import modules.schedulers.GiveawayScheduler;
import mysql.modules.giveaway.DBGiveaway;
import mysql.modules.giveaway.GiveawayData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "giveaway",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🎆",
        releaseDate = { 2020, 10, 28 },
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "giveaways" }
)
public class GiveawayCommand extends NavigationAbstract implements OnReactionListener {

    public final static int ARTICLE_LENGTH_MAX = 250;
    public final static int DESC_LENGTH_MAX = 1000;
    public final static int WINNERS_MIN = 1;
    public final static int WINNERS_MAX = 20;

    private final static int
            ADD_OR_EDIT = 0,
            ADD_MESSAGE = 1,
            EDIT_MESSAGE = 2,
            REROLL_MESSAGE = 12,
            CONFIGURE_MESSAGE = 3,
            UPDATE_TITLE = 11,
            UPDATE_DESC = 4,
            UPDATE_DURATION = 5,
            UPDATE_WINNERS = 6,
            UPDATE_EMOJI = 7,
            UPDATE_IMAGE = 8,
            EXAMPLE = 9,
            SENT = 10,
            REROLL_NUMBER = 13;

    private CustomObservableMap<Long, GiveawayData> giveawayMap = null;

    private long messageId;
    private String title = "";
    private String description = "";
    private long durationMinutes = 10080;
    private int amountOfWinners = 1;
    private Emoji emoji = Emoji.fromUnicode("🎉");
    private String imageLink;
    private LocalFile imageCdn;
    private AtomicStandardGuildMessageChannel channel;
    private Instant instant;
    private boolean editMode = false;
    private GiveawayData rerollGiveawayData;
    private int rerollWinners;

    public GiveawayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        giveawayMap = DBGiveaway.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        registerReactionListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = ADD_MESSAGE)
    public MessageInputResponse onMessageAddMessage(MessageReceivedEvent event, String input) {
        List<StandardGuildMessageChannel> channel = MentionUtil.getStandardGuildMessageChannels(event.getGuild(), input).getList();
        if (channel.size() > 0) {
            if (checkWriteEmbedInChannelWithLog(channel.get(0))) {
                this.channel = new AtomicStandardGuildMessageChannel(channel.get(0));
                setLog(LogStatus.SUCCESS, getString("channelset"));
                return MessageInputResponse.SUCCESS;
            } else {
                return MessageInputResponse.FAILED;
            }
        }
        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    @ControllerMessage(state = UPDATE_TITLE)
    public MessageInputResponse onMessageUpdateTitle(MessageReceivedEvent event, String input) {
        if (input.length() > 0 && input.length() <= ARTICLE_LENGTH_MAX) {
            title = input;
            setLog(LogStatus.SUCCESS, getString("titleset", input));
            setState(CONFIGURE_MESSAGE);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", StringUtil.numToString(ARTICLE_LENGTH_MAX)));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = UPDATE_DESC)
    public MessageInputResponse onMessageUpdateDesc(MessageReceivedEvent event, String input) {
        if (input.length() > 0 && input.length() <= DESC_LENGTH_MAX) {
            description = input;
            setLog(LogStatus.SUCCESS, getString("descriptionset", input));
            setState(CONFIGURE_MESSAGE);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "1000"));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = UPDATE_DURATION)
    public MessageInputResponse onMessageUpdateDuration(MessageReceivedEvent event, String input) {
        long minutes = MentionUtil.getTimeMinutes(input).getValue();

        if (minutes > 0) {
            final int MAX = 999 * 24 * 60;
            if (minutes <= MAX) {
                durationMinutes = minutes;
                setLog(LogStatus.SUCCESS, getString("durationset", input));
                setState(CONFIGURE_MESSAGE);
                return MessageInputResponse.SUCCESS;
            } else {
                setLog(LogStatus.FAILURE, getString("durationtoolong"));
                return MessageInputResponse.FAILED;
            }
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = UPDATE_WINNERS)
    public MessageInputResponse onMessageUpdateWinners(MessageReceivedEvent event, String input) {
        int amount;
        if (StringUtil.stringIsInt(input) &&
                (amount = Integer.parseInt(input)) >= WINNERS_MIN &&
                amount <= WINNERS_MAX
        ) {
            amountOfWinners = amount;
            setLog(LogStatus.SUCCESS, getString("winnersset", input));
            setState(CONFIGURE_MESSAGE);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", String.valueOf(WINNERS_MIN), String.valueOf(WINNERS_MAX)));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = UPDATE_EMOJI)
    public MessageInputResponse onMessageUpdateEmoji(MessageReceivedEvent event, String input) {
        List<Emoji> emojiList = MentionUtil.getEmojis(event.getMessage(), input).getList();
        if (emojiList.size() > 0) {
            Emoji emoji = emojiList.get(0);
            return processEmoji(emoji) ? MessageInputResponse.SUCCESS : MessageInputResponse.FAILED;
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    @ControllerMessage(state = UPDATE_IMAGE)
    public MessageInputResponse onMessageUpdateImage(MessageReceivedEvent event, String input) {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.size() > 0) {
            Message.Attachment attachment = attachments.get(0);
            LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("giveaway/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
            boolean success = FileUtil.downloadImageAttachment(attachment, tempFile);
            if (success) {
                imageLink = uploadFile(tempFile);
                setLog(LogStatus.SUCCESS, getString("imageset"));
                setState(CONFIGURE_MESSAGE);
                return MessageInputResponse.SUCCESS;
            }
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    @ControllerMessage(state = REROLL_NUMBER)
    public MessageInputResponse onMessageRerollWinners(MessageReceivedEvent event, String input) {
        long amount = MentionUtil.getAmountExt(input);
        if (amount >= WINNERS_MIN && amount <= WINNERS_MAX) {
            rerollWinners = (int) amount;
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", String.valueOf(WINNERS_MIN), String.valueOf(WINNERS_MAX)));
            return MessageInputResponse.FAILED;
        }
    }

    private String uploadFile(LocalFile file) {
        if (imageCdn != null) {
            imageCdn.delete();
        }

        imageCdn = file;
        return file.cdnGetUrl();
    }

    @ControllerButton(state = ADD_OR_EDIT)
    public boolean onButtonAddOrEdit(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                setState(ADD_MESSAGE);
                editMode = false;
                return true;

            case 1:
                if (getActiveGiveawaySlots().size() > 0) {
                    setState(EDIT_MESSAGE);
                    editMode = true;
                } else {
                    setLog(LogStatus.FAILURE, getString("nothing"));
                }
                return true;

            case 2:
                if (getCompletedGiveawaySlots().size() > 0) {
                    setState(REROLL_MESSAGE);
                } else {
                    setLog(LogStatus.FAILURE, getString("nothing_completed"));
                }
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = ADD_MESSAGE)
    public boolean onButtonAddMessage(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(ADD_OR_EDIT);
                return true;

            case 0:
                if (channel != null) {
                    setState(CONFIGURE_MESSAGE);
                    return true;
                }

            default:
                return false;
        }
    }

    @ControllerButton(state = EDIT_MESSAGE)
    public boolean onButtonEditMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(ADD_OR_EDIT);
            return true;
        }

        List<GiveawayData> giveaways = getActiveGiveawaySlots();
        if (i >= 0 && i < giveaways.size()) {
            GiveawayData giveaway = giveaways.get(i);
            messageId = giveaway.getMessageId();
            title = giveaway.getTitle();
            description = giveaway.getDescription();
            durationMinutes = giveaway.getDurationMinutes();
            amountOfWinners = giveaway.getWinners();
            imageLink = giveaway.getImageUrl().orElse(null);
            channel = new AtomicStandardGuildMessageChannel(event.getGuild().getIdLong(), giveaway.getStandardGuildMessageChannelId());
            instant = giveaway.getStart();
            emoji = Emoji.fromFormatted(giveaway.getEmoji());
            setState(CONFIGURE_MESSAGE);

            return true;
        }

        return false;
    }

    @ControllerButton(state = REROLL_MESSAGE)
    public boolean onButtonRerollMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(ADD_OR_EDIT);
            return true;
        }

        List<GiveawayData> giveaways = getCompletedGiveawaySlots();
        if (i >= 0 && i < giveaways.size()) {
            rerollGiveawayData = giveaways.get(i);
            rerollWinners = 0;
            setState(REROLL_NUMBER);
            return true;
        }

        return false;
    }

    @ControllerButton(state = CONFIGURE_MESSAGE)
    public boolean onButtonConfigureMessage(ButtonInteractionEvent event, int i) throws ExecutionException, InterruptedException {
        switch (i) {
            case -1:
                if (!editMode) {
                    setState(ADD_MESSAGE);
                } else {
                    setState(EDIT_MESSAGE);
                }
                return true;

            case 0:
                setState(UPDATE_TITLE);
                return true;

            case 1:
                setState(UPDATE_DESC);
                return true;

            case 2:
                if (!editMode) {
                    setState(UPDATE_DURATION);
                } else {
                    setLog(LogStatus.FAILURE, getString("locked"));
                }
                return true;

            case 3:
                setState(UPDATE_WINNERS);
                return true;

            case 4:
                if (!editMode) {
                    setState(UPDATE_EMOJI);
                } else {
                    setLog(LogStatus.FAILURE, getString("locked"));
                }
                return true;

            case 5:
                setState(UPDATE_IMAGE);
                return true;

            case 6:
                setState(EXAMPLE);
                return true;

            case 7:
                if (!title.isEmpty()) {
                    send(event, editMode);
                } else {
                    setLog(LogStatus.FAILURE, getString("noitem"));
                }
                return true;

            case 8:
                if (editMode) {
                    if (!title.isEmpty()) {
                        send(event, false);
                    } else {
                        setLog(LogStatus.FAILURE, getString("noitem"));
                    }
                    return true;
                } else {
                    return false;
                }

            default:
                return false;
        }
    }

    private void send(ButtonInteractionEvent event, boolean endPrematurely) {
        if (editMode && (!giveawayMap.containsKey(messageId) || !giveawayMap.get(messageId).isActive())) {
            setLog(LogStatus.FAILURE, getString("dashboard_toolate"));
            setState(EDIT_MESSAGE);
        }

        Optional<Long> messageIdOpt = sendMessage();
        if (messageIdOpt.isEmpty() && editMode) {
            setLog(LogStatus.FAILURE, getString("nomessage"));
            setState(EDIT_MESSAGE);
            return;
        }

        if (messageIdOpt.isPresent()) {
            setState(SENT);
            deregisterListeners();
            GiveawayData giveawayData = new GiveawayData(
                    event.getGuild().getIdLong(),
                    channel.getIdLong(),
                    messageIdOpt.get(),
                    emoji.getFormatted(),
                    amountOfWinners,
                    instant,
                    endPrematurely ? 0 : durationMinutes,
                    title,
                    description,
                    imageLink,
                    true
            );
            if (endPrematurely || !giveawayMap.containsKey(giveawayData.getMessageId())) {
                GiveawayScheduler.loadGiveawayBean(giveawayData);
            }
            giveawayMap.put(giveawayData.getMessageId(), giveawayData);
        } else {
            setLog(LogStatus.FAILURE, getString("error"));
        }
    }

    @Override
    public boolean onReaction(@NotNull GenericMessageReactionEvent event) throws Throwable {
        if (getState() == UPDATE_EMOJI && event instanceof MessageReactionAddEvent) {
            processEmoji(event.getEmoji());
            processDraw(event.getMember(), true).exceptionally(ExceptionLogger.get());
            if (BotPermissionUtil.can(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                event.getReaction().removeReaction(event.getUser()).queue();
            }
            return false;
        }
        return false;
    }

    @ControllerButton(state = UPDATE_IMAGE)
    public boolean onButtonUpdateImage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        } else if (i == 0) {
            if (imageCdn != null) {
                imageCdn.delete();
                imageCdn = null;
            }
            imageLink = null;
            setLog(LogStatus.SUCCESS, getString("imageset"));
            setState(CONFIGURE_MESSAGE);
            return true;
        }

        return false;
    }

    @ControllerButton(state = SENT)
    public boolean onButtonSent(ButtonInteractionEvent event, int i) {
        return false;
    }

    @ControllerButton(state = REROLL_NUMBER)
    public boolean onButtonRerollNumber(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(REROLL_MESSAGE);
            return true;
        } else if (i == 0 && rerollWinners > 0) {
            boolean messageExists = GiveawayScheduler.processGiveawayUsers(rerollGiveawayData, rerollWinners, true).join();
            if (messageExists) {
                setLog(LogStatus.SUCCESS, getString("rerollset", rerollGiveawayData.getTitle()));
            } else {
                setLog(LogStatus.FAILURE, getString("error"));
            }
            setState(REROLL_MESSAGE);
            return true;
        } else if (i == (rerollWinners > 0 ? 1 : 0)) {
            giveawayMap.remove(rerollGiveawayData.getMessageId());
            setLog(LogStatus.SUCCESS, getString("removed", rerollGiveawayData.getTitle()));
            setState(REROLL_MESSAGE);
            return true;
        }
        return false;
    }

    @ControllerButton
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    private boolean processEmoji(Emoji emoji) {
        if (emoji instanceof UnicodeEmoji || ShardManager.customEmojiIsKnown((CustomEmoji) emoji)) {
            this.emoji = emoji;
            setLog(LogStatus.SUCCESS, getString("emojiset"));
            setState(CONFIGURE_MESSAGE);
            return true;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "emojiunknown", emoji.getName()));
            return false;
        }
    }

    @Draw(state = ADD_OR_EDIT)
    public EmbedBuilder onDrawAddOrEdit(Member member) {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = ADD_MESSAGE)
    public EmbedBuilder onDrawAddMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (channel != null) {
            setComponents(TextManager.getString(getLocale(), TextManager.GENERAL, "continue"));
        }
        return EmbedFactory.getEmbedDefault(this, getString("state1_description", Optional.ofNullable(channel).map(MentionableAtomicAsset::getPrefixedNameInField).orElse(notSet)), getString("state1_title"));
    }

    @Draw(state = EDIT_MESSAGE)
    public EmbedBuilder onDrawEditMessage(Member member) {
        String[] options = getActiveGiveawaySlots().stream()
                .map(giveawayData -> {
                    AtomicStandardGuildMessageChannel atomicStandardGuildMessageChannel = new AtomicStandardGuildMessageChannel(giveawayData.getGuildId(), giveawayData.getStandardGuildMessageChannelId());
                    return getString("state2_slot", giveawayData.getTitle(), atomicStandardGuildMessageChannel.getName());
                })
                .toArray(String[]::new);
        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = REROLL_MESSAGE)
    public EmbedBuilder onDrawRerollMessage(Member member) {
        String[] options = getCompletedGiveawaySlots().stream()
                .map(giveawayData -> getString("state2_slot", giveawayData.getTitle(), new AtomicStandardGuildMessageChannel(member.getGuild().getIdLong(), giveawayData.getStandardGuildMessageChannelId()).getName()))
                .toArray(String[]::new);
        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state12_description"), getString("state12_title"));
    }

    @Draw(state = CONFIGURE_MESSAGE)
    public EmbedBuilder onDrawConfigureMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (editMode) {
            setComponents(getString("state3_options_edit").split("\n"));
        } else {
            setComponents(getString("state3_options").split("\n"));
        }

        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title_" + (editMode ? "edit" : "new")))
                .addField(getString("state3_mtitle"), StringUtil.escapeMarkdown(title.isEmpty() ? notSet : title), false)
                .addField(getString("state3_mdescription"), StringUtil.escapeMarkdown(description.isEmpty() ? notSet : description), false)
                .addField(getString("state3_mduration"), TimeUtil.getRemainingTimeString(getLocale(), durationMinutes * 60_000, false), true)
                .addField(getString("state3_mwinners"), String.valueOf(amountOfWinners), true)
                .addField(getString("state3_memoji"), emoji.getFormatted(), true)
                .addField(getString("state3_mimage"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), imageLink != null), true);
    }

    @Draw(state = UPDATE_TITLE)
    public EmbedBuilder onDrawUpdateTitle(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state11_description"), getString("state11_title"));
    }

    @Draw(state = UPDATE_DESC)
    public EmbedBuilder onDrawUpdateDesc(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

    @Draw(state = UPDATE_DURATION)
    public EmbedBuilder onDrawUpdateDuration(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state5_description"), getString("state5_title"));
    }

    @Draw(state = UPDATE_WINNERS)
    public EmbedBuilder onDrawUpdateWinners(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state6_description"), getString("state6_title"));
    }

    @Draw(state = UPDATE_EMOJI)
    public EmbedBuilder onDrawUpdateEmoji(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));
    }

    @Draw(state = UPDATE_IMAGE)
    public EmbedBuilder onDrawUpdateImage(Member member) {
        setComponents(getString("state8_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state8_description"), getString("state8_title"));
    }

    @Draw(state = EXAMPLE)
    public EmbedBuilder onDrawExample(Member member) {
        return Giveaway.getMessageEmbed(getLocale(), title, description, amountOfWinners, emoji,
                durationMinutes, imageLink, Instant.now()
        );
    }

    @Draw(state = SENT)
    public EmbedBuilder onDrawSent(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state10_description"), getString("state10_title"));
    }

    @Draw(state = REROLL_NUMBER)
    public EmbedBuilder onDrawRerollNumber(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        Button[] buttons;
        if (rerollWinners > 0) {
            buttons = new Button[] {
                    Button.of(ButtonStyle.PRIMARY, "0", getString("state13_confirm")),
                    Button.of(ButtonStyle.DANGER, "1", getString("state13_delete"))
            };
        } else {
            buttons = new Button[] {
                    Button.of(ButtonStyle.DANGER, "0", getString("state13_delete")),
            };
        }

        setComponents(buttons);
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state13_description", rerollGiveawayData.getTitle(), rerollWinners > 0 ? StringUtil.numToString(rerollWinners) : notSet),
                getString("state13_title")
        );
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
            StandardGuildMessageChannel channel = this.channel.get().get();
            if (!editMode) {
                instant = Instant.now();
                EmbedBuilder eb = Giveaway.getMessageEmbed(getLocale(), title, description, amountOfWinners, emoji,
                        durationMinutes, imageLink, instant
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
                EmbedBuilder eb = Giveaway.getMessageEmbed(getLocale(), title, description, amountOfWinners, emoji,
                        durationMinutes, imageLink, instant
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
