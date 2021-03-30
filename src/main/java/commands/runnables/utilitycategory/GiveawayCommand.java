package commands.runnables.utilitycategory;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import constants.Response;
import core.*;
import core.atomicassets.AtomicTextChannel;
import core.atomicassets.MentionableAtomicAsset;
import core.utils.*;
import modules.schedulers.GiveawayScheduler;
import mysql.modules.giveaway.DBGiveaway;
import mysql.modules.giveaway.GiveawaySlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "giveaway",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🎆",
        releaseDate = { 2020, 10, 28 },
        executableWithoutArgs = true,
        aliases = { "giveaways" }
)
public class GiveawayCommand extends NavigationAbstract {

    private final static int
            ADD_OR_EDIT = 0,
            ADD_MESSAGE = 1,
            EDIT_MESSAGE = 2,
            CONFIGURE_MESSAGE = 3,
            UPDATE_TITLE = 11,
            UPDATE_DESC = 4,
            UPDATE_DURATION = 5,
            UPDATE_WINNERS = 6,
            UPDATE_EMOJI = 7,
            UPDATE_IMAGE = 8,
            EXAMPLE = 9,
            SENT = 10;

    private CustomObservableMap<Long, GiveawaySlot> giveawayBeans = null;

    private long messageId;
    private String title;
    private String description = "";
    private long durationMinutes = 10080;
    private int amountOfWinners = 1;
    private String emoji = "🎉";
    private String imageLink;
    private LocalFile imageCdn;
    private AtomicTextChannel channel;
    private Instant instant;
    private boolean editMode = false;

    public GiveawayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        giveawayBeans = DBGiveaway.getInstance().retrieve(event.getGuild().getIdLong());
        title = getString("title");
        registerNavigationListener(12);
        return true;
    }

    @ControllerMessage(state = ADD_MESSAGE)
    public Response onMessageAddMessage(GuildMessageReceivedEvent event, String input) {
        List<TextChannel> serverTextChannel = MentionUtil.getTextChannels(event.getMessage(), input).getList();
        if (serverTextChannel.size() > 0) {
            if (checkWriteInChannelWithLog(serverTextChannel.get(0))) {
                channel = new AtomicTextChannel(serverTextChannel.get(0));
                setLog(LogStatus.SUCCESS, getString("channelset"));
                return Response.TRUE;
            } else {
                return Response.FALSE;
            }
        }
        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return Response.FALSE;
    }

    @ControllerMessage(state = UPDATE_TITLE)
    public Response onMessageUpdateTitle(GuildMessageReceivedEvent event, String input) {
        if (input.length() > 0 && input.length() <= 250) {
            title = input;
            setLog(LogStatus.SUCCESS, getString("titleset", input));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "250"));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_DESC)
    public Response onMessageUpdateDesc(GuildMessageReceivedEvent event, String input) {
        if (input.length() > 0 && input.length() <= 1000) {
            description = input;
            setLog(LogStatus.SUCCESS, getString("descriptionset", input));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "1000"));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_DURATION)
    public Response onMessageUpdateDuration(GuildMessageReceivedEvent event, String input) {
        long minutes = MentionUtil.getTimeMinutesExt(input);

        if (minutes > 0) {
            final int MAX = 999 * 24 * 60;
            if (minutes <= MAX) {
                durationMinutes = minutes;
                setLog(LogStatus.SUCCESS, getString("durationset", input));
                setState(CONFIGURE_MESSAGE);
                return Response.TRUE;
            } else {
                setLog(LogStatus.FAILURE, getString("durationtoolong"));
                return Response.FALSE;
            }
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_WINNERS)
    public Response onMessageUpdateWinners(GuildMessageReceivedEvent event, String input) {
        final int MIN = 1, MAX = 20;
        int amount;
        if (StringUtil.stringIsInt(input) &&
                (amount = Integer.parseInt(input)) >= MIN &&
                amount <= MAX
        ) {
            amountOfWinners = amount;
            setLog(LogStatus.SUCCESS, getString("winnersset", input));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", String.valueOf(MIN), String.valueOf(MAX)));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_EMOJI)
    public Response onMessageUpdateEmoji(GuildMessageReceivedEvent event, String input) {
        List<String> emojiList = MentionUtil.getEmojis(event.getMessage(), input).getList();
        if (emojiList.size() > 0) {
            String emoji = emojiList.get(0);
            return processEmoji(emoji) ? Response.TRUE : Response.FALSE;
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return Response.FALSE;
    }

    @ControllerMessage(state = UPDATE_IMAGE)
    public Response onMessageUpdateImage(GuildMessageReceivedEvent event, String input) throws IOException, ExecutionException, InterruptedException {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.size() > 0) {
            LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("giveaway/%d.png", System.nanoTime()));
            boolean success = FileUtil.downloadImageAttachment(attachments.get(0), tempFile);
            if (success) {
                imageLink = uploadFile(tempFile);
                setLog(LogStatus.SUCCESS, getString("imageset"));
                setState(CONFIGURE_MESSAGE);
                return Response.TRUE;
            }
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return Response.FALSE;
    }

    private String uploadFile(LocalFile file) {
        if (imageCdn != null) {
            imageCdn.delete();
            imageCdn = null;
        }

        imageCdn = file;
        return file.cdnGetUrl();
    }

    @ControllerReaction(state = ADD_OR_EDIT)
    public boolean onReactionAddOrEdit(GenericGuildMessageReactionEvent event, int i) {
        switch (i) {
            case -1:
                removeNavigationWithMessage();
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

            default:
                return false;
        }
    }

    @ControllerReaction(state = ADD_MESSAGE)
    public boolean onReactionAddMessage(GenericGuildMessageReactionEvent event, int i) {
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

    @ControllerReaction(state = EDIT_MESSAGE)
    public boolean onReactionEditMessage(GenericGuildMessageReactionEvent event, int i) {
        if (i == -1) {
            setState(ADD_OR_EDIT);
            return true;
        }

        List<GiveawaySlot> giveaways = getActiveGiveawaySlots();
        if (i >= 0 && i < giveaways.size()) {
            GiveawaySlot giveaway = giveaways.get(i);
            messageId = giveaway.getMessageId();
            title = giveaway.getTitle();
            description = giveaway.getDescription();
            durationMinutes = giveaway.getDurationMinutes();
            amountOfWinners = giveaway.getWinners();
            imageLink = giveaway.getImageUrl().orElse(null);
            channel = new AtomicTextChannel(event.getGuild().getTextChannelById(giveaway.getTextChannelId()));
            instant = giveaway.getStart();
            emoji = giveaway.getEmoji();
            setState(CONFIGURE_MESSAGE);

            return true;
        }

        return false;
    }

    @ControllerReaction(state = CONFIGURE_MESSAGE)
    public boolean onReactionConfigureMessage(GenericGuildMessageReactionEvent event, int i) throws ExecutionException, InterruptedException {
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
                Optional<Long> messageIdOpt = sendMessage();
                if (messageIdOpt.isPresent()) {
                    setState(SENT);
                    removeNavigation();
                    GiveawaySlot giveawaySlot = new GiveawaySlot(
                            event.getGuild().getIdLong(),
                            channel.getIdLong(),
                            messageIdOpt.get(),
                            emoji,
                            amountOfWinners,
                            instant,
                            durationMinutes,
                            title,
                            description,
                            imageLink,
                            true
                    );
                    if (!giveawayBeans.containsKey(giveawaySlot.getMessageId())) {
                        GiveawayScheduler.getInstance().loadGiveawayBean(giveawaySlot);
                    }

                    giveawayBeans.put(giveawaySlot.getMessageId(), giveawaySlot);
                } else {
                    setLog(LogStatus.FAILURE, getString("error"));
                }
                return true;

            default:
                return false;
        }
    }

    @ControllerReaction(state = UPDATE_EMOJI)
    public boolean onReactionUpdateEmoji(GenericGuildMessageReactionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
        } else {
            event.getReaction().removeReaction(event.getUser()).queue();
            processEmoji(EmojiUtil.reactionEmoteAsMention(event.getReactionEmote()));
        }

        return true;
    }

    @ControllerReaction(state = UPDATE_IMAGE)
    public boolean onReactionUpdateImage(GenericGuildMessageReactionEvent event, int i) {
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

    @ControllerReaction(state = SENT)
    public boolean onReactionSent(GenericGuildMessageReactionEvent event, int i) {
        return false;
    }

    @ControllerReaction
    public boolean onReactionDefault(GenericGuildMessageReactionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    private boolean processEmoji(String emoji) {
        if (EmojiUtil.emojiIsUnicode(emoji) || ShardManager.getInstance().emoteIsKnown(emoji)) {
            this.emoji = emoji;
            setLog(LogStatus.SUCCESS, getString("emojiset"));
            setState(CONFIGURE_MESSAGE);
            return true;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "emojiunknown"));
            return false;
        }
    }

    @Draw(state = ADD_OR_EDIT)
    public EmbedBuilder onDrawAddOrEdit() {
        setOptions(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = ADD_MESSAGE)
    public EmbedBuilder onDrawAddMessage() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (channel != null) {
            setOptions(new String[] { TextManager.getString(getLocale(), TextManager.GENERAL, "continue") });
        }
        return EmbedFactory.getEmbedDefault(this, getString("state1_description", Optional.ofNullable(channel).map(MentionableAtomicAsset::getAsMention).orElse(notSet)), getString("state1_title"));
    }

    @Draw(state = EDIT_MESSAGE)
    public EmbedBuilder onDrawEditMessage() {
        String[] options = new ListGen<GiveawaySlot>()
                .getList(getActiveGiveawaySlots(), ListGen.SLOT_TYPE_NONE, giveawaySlot -> getString("state2_slot", giveawaySlot.getTitle(), giveawaySlot.getTextChannel().get().getAsMention()))
                .split("\n");
        setOptions(options);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = CONFIGURE_MESSAGE)
    public EmbedBuilder onDrawConfigureMessage() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setOptions(getString("state3_options").split("\n"));

        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title_" + (editMode ? "edit" : "new")))
                .addField(getString("state3_mtitle"), title, false)
                .addField(getString("state3_mdescription"), StringUtil.escapeMarkdown(description.isEmpty() ? notSet : description), false)
                .addField(getString("state3_mduration"), TimeUtil.getRemainingTimeString(getLocale(), durationMinutes * 60_000, false), true)
                .addField(getString("state3_mwinners"), String.valueOf(amountOfWinners), true)
                .addField(getString("state3_memoji"), emoji, true)
                .addField(getString("state3_mimage"), StringUtil.getEmojiForBoolean(imageLink != null), true);
    }

    @Draw(state = UPDATE_TITLE)
    public EmbedBuilder onDrawUpdateTitle() {
        return EmbedFactory.getEmbedDefault(this, getString("state11_description"), getString("state11_title"));
    }

    @Draw(state = UPDATE_DESC)
    public EmbedBuilder onDrawUpdateDesc() {
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

    @Draw(state = UPDATE_DURATION)
    public EmbedBuilder onDrawUpdateDuration() {
        return EmbedFactory.getEmbedDefault(this, getString("state5_description"), getString("state5_title"));
    }

    @Draw(state = UPDATE_WINNERS)
    public EmbedBuilder onDrawUpdateWinners() {
        return EmbedFactory.getEmbedDefault(this, getString("state6_description"), getString("state6_title"));
    }

    @Draw(state = UPDATE_EMOJI)
    public EmbedBuilder onDrawUpdateEmoji() {
        return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));
    }

    @Draw(state = UPDATE_IMAGE)
    public EmbedBuilder onDrawUpdateImage() {
        setOptions(getString("state8_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state8_description"), getString("state8_title"));
    }

    @Draw(state = EXAMPLE)
    public EmbedBuilder onDrawExample() {
        return getMessageEmbed();
    }

    @Draw(state = SENT)
    public EmbedBuilder onDrawSent() {
        return EmbedFactory.getEmbedDefault(this, getString("state10_description"), getString("state10_title"));
    }

    private List<GiveawaySlot> getActiveGiveawaySlots() {
        return giveawayBeans.values().stream()
                .filter(g -> g.isActive() && g.getEnd().isAfter(Instant.now()))
                .collect(Collectors.toList());
    }

    private Optional<Long> sendMessage() {
        Message message;
        if (checkWriteInChannelWithLog(channel.get().orElse(null))) {
            TextChannel textChannel = channel.get().get();
            if (!editMode) {
                instant = Instant.now();
                message = textChannel.sendMessage(getMessageEmbed().build()).complete();
                if (BotPermissionUtil.canReadHistory(textChannel, Permission.MESSAGE_ADD_REACTION)) {
                    message.addReaction(EmojiUtil.emojiAsReactionTag(emoji)).queue();
                }
                return Optional.of(message.getIdLong());
            } else {
                if (instant.plus(durationMinutes, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                    return Optional.empty();
                }
                try {
                    textChannel.editMessageById(messageId, getMessageEmbed().build()).complete();
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

    private EmbedBuilder getMessageEmbed() {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        Instant startInstant = editMode ? instant : Instant.now();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(getCommandProperties().emoji() + " " + title)
                .setDescription(description)
                .setFooter(getString("endson"))
                .setTimestamp(startInstant.plus(durationMinutes, ChronoUnit.MINUTES));

        if (description.isEmpty()) {
            eb.setDescription(getString("tutorial", amountOfWinners != 1, emoji, String.valueOf(amountOfWinners)));
        } else {
            eb.addField(
                    Emojis.EMPTY_EMOJI,
                    getString("tutorial", amountOfWinners != 1, emoji, String.valueOf(amountOfWinners)),
                    false
            );
        }

        if (imageLink != null) {
            eb.setImage(imageLink);
        }
        return eb;
    }

}
