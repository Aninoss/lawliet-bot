package commands.runnables.utilitycategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import constants.Emojis;
import constants.LogStatus;
import constants.Permission;
import constants.Response;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.IDGenerator;
import core.TextManager;
import core.utils.FileUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.emoji.Emoji;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "giveaway",
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "🎆",
        executableWithoutArgs = true,
        aliases = { "giveaways" }
)
public class GiveawayCommand extends Command implements OnNavigationListener {

    private static final int MAX_GIVEAWAYS = 20;
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

    private long id;
    private String title;
    private String description;
    private long durationMinutes = 10080;
    private int amountOfWinners = 1;
    private Emoji emoji = StringUtil.unicodeToEmoji("✉️");
    private String imageLink;
    private Message imageMessage;
    private ServerTextChannel channel;
    private boolean editMode = false;
    private Message editMessage;

    public GiveawayCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        title = getString("title");
        return true;
    }

    @ControllerMessage(state = ADD_MESSAGE)
    public Response onMessageAddMessage(MessageCreateEvent event, String inputString) {
        ArrayList<ServerTextChannel> serverTextChannel = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
        if (serverTextChannel.size() > 0) {
            if (checkWriteInChannelWithLog(serverTextChannel.get(0))) {
                channel = serverTextChannel.get(0);
                setLog(LogStatus.SUCCESS, getString("channelset"));
                return Response.TRUE;
            } else {
                return Response.FALSE;
            }
        }
        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
        return Response.FALSE;
    }

    @ControllerMessage(state = UPDATE_TITLE)
    public Response onMessageUpdateTitle(MessageCreateEvent event, String inputString) {
        if (inputString.length() > 0 && inputString.length() <= 250) {
            title = inputString;
            setLog(LogStatus.SUCCESS, getString("titleset", inputString));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "250"));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_DESC)
    public Response onMessageUpdateDesc(MessageCreateEvent event, String inputString) {
        if (inputString.length() > 0 && inputString.length() <= 1000) {
            description = inputString;
            setLog(LogStatus.SUCCESS, getString("descriptionset", inputString));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", "1000"));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_DURATION)
    public Response onMessageUpdateDuration(MessageCreateEvent event, String inputString) {
        long minutes = MentionUtil.getTimeMinutesExt(inputString);

        if (minutes > 0) {
            final int MAX = 999 * 24 * 60;
            if (minutes <= MAX) {
                durationMinutes = minutes;
                setLog(LogStatus.SUCCESS, getString("durationset", inputString));
                setState(CONFIGURE_MESSAGE);
                return Response.TRUE;
            } else {
                setLog(LogStatus.FAILURE, getString("durationtoolong"));
                return Response.FALSE;
            }
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", inputString));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_WINNERS)
    public Response onMessageUpdateWinners(MessageCreateEvent event, String inputString) {
        final int MIN = 1, MAX = 20;
        int amount;
        if (StringUtil.stringIsInt(inputString) &&
                (amount = Integer.parseInt(inputString)) >= MIN &&
                amount <= MAX
        ) {
            amountOfWinners = amount;
            setLog(LogStatus.SUCCESS, getString("winnersset", inputString));
            setState(CONFIGURE_MESSAGE);
            return Response.TRUE;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number2", String.valueOf(MIN), String.valueOf(MAX)));
            return Response.FALSE;
        }
    }

    @ControllerMessage(state = UPDATE_EMOJI)
    public Response onMessageUpdateEmoji(MessageCreateEvent event, String inputString) {
        List<Emoji> emojiList = MentionUtil.getEmojis(event.getMessage(), event.getMessageContent()).getList();
        if (emojiList.size() > 0) {
            Emoji emoji = emojiList.get(0);
            return processEmoji(emoji) ? Response.TRUE : Response.FALSE;
        }

        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
        return Response.FALSE;
    }

    @ControllerMessage(state = UPDATE_IMAGE)
    public Response onMessageUpdateImage(MessageCreateEvent event, String inputString) throws IOException, ExecutionException, InterruptedException {
        List<MessageAttachment> attachments = event.getMessage().getAttachments();
        if (attachments.size() > 0) {
            Optional<File> file = FileUtil.downloadMessageAttachment(attachments.get(0), String.format("temp/%d.png", id));
            if (file.isPresent()) {
                imageLink = uploadFile(file.get());
                file.get().delete();
                setLog(LogStatus.SUCCESS, getString("imageset"));
                setState(CONFIGURE_MESSAGE);
                return Response.TRUE;
            }
        }

        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
        return Response.FALSE;
    }

    private String uploadFile(File file) throws ExecutionException, InterruptedException {
        if (imageMessage != null) {
            imageMessage.delete().exceptionally(ExceptionLogger.get());
            imageMessage = null;
        }

        imageMessage = DiscordApiCollection.getInstance().getHomeServer()
                .getTextChannelById(767039446285156372L).get()
                .sendMessage(file).get();
        return imageMessage.getAttachments().get(0).getUrl().toString();
    }

    @ControllerReaction(state = ADD_OR_EDIT)
    public boolean onReactionAddOrEdit(SingleReactionEvent event, int i) {
        switch (i) {
            case -1:
                removeNavigationWithMessage();
                return false;

            case 0:
                setState(ADD_MESSAGE);
                editMode = false;
                return true;

            case 1:
                setState(EDIT_MESSAGE);
                editMode = true;
                return true;

            default:
                return false;
        }
    }

    @ControllerReaction(state = ADD_MESSAGE)
    public boolean onReactionAddMessage(SingleReactionEvent event, int i) {
        switch (i) {
            case -1:
                setState(ADD_OR_EDIT);
                return true;

            case 0:
                if (channel != null) {
                    id = IDGenerator.getInstance().getId();
                    setState(CONFIGURE_MESSAGE);
                    return true;
                }

            default:
                return false;
        }
    }

    /*@ControllerReaction(state = EDIT_MESSAGE)
    public boolean onReactionEditMessage(SingleReactionEvent event, int i) {
        switch (i) {
            case -1:
                setState(ADD_OR_EDIT);
                return true;

            case 0:
                if (editMessage != null) {
                    updateValuesFromMessage(editMessage);
                    setState(CONFIGURE_MESSAGE);
                    return true;
                }

            default:
                return false;
        }
    }*/

    @ControllerReaction(state = CONFIGURE_MESSAGE)
    public boolean onReactionConfigureMessage(SingleReactionEvent event, int i) throws ExecutionException, InterruptedException {
        switch (i) {
            case -1:
                if (!editMode) setState(ADD_MESSAGE);
                else setState(EDIT_MESSAGE);
                return true;

            case 0:
                setState(UPDATE_TITLE);
                return true;

            case 1:
                setState(UPDATE_DESC);
                return true;

            case 2:
                setState(UPDATE_DURATION);
                return true;

            case 3:
                setState(UPDATE_WINNERS);
                return true;

            case 4:
                setState(UPDATE_EMOJI);
                return true;

            case 5:
                setState(UPDATE_IMAGE);
                return true;

            case 6:
                setState(EXAMPLE);
                return true;

            case 7:
                Optional<Message> messageOpt = sendMessage();
                if (messageOpt.isPresent()) {
                    setState(SENT);
                    removeNavigation();
                }
                return true;

            default:
                return false;
        }
    }

    @ControllerReaction(state = UPDATE_EMOJI)
    public boolean onReactionUpdateEmoji(SingleReactionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
        } else {
            event.getReaction().ifPresent(Reaction::remove);
            processEmoji(event.getEmoji());
        }

        return true;
    }

    @ControllerReaction(state = UPDATE_IMAGE)
    public boolean onReactionUpdateImage(SingleReactionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        } else if (i == 0) {
            if (imageMessage != null) {
                imageMessage.delete().exceptionally(ExceptionLogger.get());
                imageMessage = null;
            }
            imageLink = null;
            setLog(LogStatus.SUCCESS, getString("imageset"));
            setState(CONFIGURE_MESSAGE);
            return true;
        }

        return false;
    }

    @ControllerReaction(state = SENT)
    public boolean onReactionSent(SingleReactionEvent event, int i) {
        return false;
    }

    @ControllerReaction
    public boolean onReactionDefault(SingleReactionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    private boolean processEmoji(Emoji emoji) {
        if (emoji.isUnicodeEmoji() || emoji.isKnownCustomEmoji()) {
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
    public EmbedBuilder onDrawAddOrEdit(DiscordApi api) {
        setOptions(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = ADD_MESSAGE)
    public EmbedBuilder onDrawAddMessage(DiscordApi api) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (channel != null) setOptions(new String[]{TextManager.getString(getLocale(),TextManager.GENERAL,"continue")});
        return EmbedFactory.getEmbedDefault(this, getString("state1_description", Optional.ofNullable(channel).map(Mentionable::getMentionTag).orElse(notSet)), getString("state1_title"));
    }

    @Draw(state = EDIT_MESSAGE)
    public EmbedBuilder onDrawEditMessage(DiscordApi api) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (editMessage != null) setOptions(new String[]{TextManager.getString(getLocale(),TextManager.GENERAL,"continue")});
        return EmbedFactory.getEmbedDefault(this, getString("state2_description", Optional.ofNullable(editMessage).map(DiscordEntity::getIdAsString).orElse(notSet)), getString("state2_title"));
    }

    @Draw(state = CONFIGURE_MESSAGE)
    public EmbedBuilder onDrawConfigureMessage(DiscordApi api) throws IOException {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setOptions(getString("state3_options").split("\n"));

        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title_" + (editMode ? "edit" : "new")))
                .addField(getString("state3_mtitle"), title, false)
                .addField(getString("state3_mdescription"), StringUtil.escapeMarkdown(Optional.ofNullable(description).orElse(notSet)), false)
                .addField(getString("state3_mduration"), TimeUtil.getRemainingTimeString(getLocale(), durationMinutes * 60_000, false), true)
                .addField(getString("state3_mwinners"), String.valueOf(amountOfWinners), true)
                .addField(getString("state3_memoji"), emoji.getMentionTag(), true)
                .addField(getString("state3_mimage"), StringUtil.getEmojiForBoolean(imageLink != null), true);
    }

    @Draw(state = UPDATE_TITLE)
    public EmbedBuilder onDrawUpdateTitle(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state11_description"), getString("state11_title"));
    }

    @Draw(state = UPDATE_DESC)
    public EmbedBuilder onDrawUpdateDesc(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

    @Draw(state = UPDATE_DURATION)
    public EmbedBuilder onDrawUpdateDuration(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state5_description"), getString("state5_title"));
    }

    @Draw(state = UPDATE_WINNERS)
    public EmbedBuilder onDrawUpdateWinners(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state6_description"), getString("state6_title"));
    }

    @Draw(state = UPDATE_EMOJI)
    public EmbedBuilder onDrawUpdateEmoji(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));
    }

    @Draw(state = UPDATE_IMAGE)
    public EmbedBuilder onDrawUpdateImage(DiscordApi api) {
        setOptions(getString("state8_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state8_description"), getString("state8_title"));
    }

    @Draw(state = EXAMPLE)
    public EmbedBuilder onDrawExample(DiscordApi api) {
        return getMessageEmbed();
    }

    @Draw(state = SENT)
    public EmbedBuilder onDrawSent(DiscordApi api) {
        return EmbedFactory.getEmbedDefault(this, getString("state10_description"), getString("state10_title"));
    }

    @Override
    public void onNavigationTimeOut(Message message) {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    private Optional<Message> sendMessage() throws ExecutionException, InterruptedException {
        Message message;
        if (!editMode) {
            if (checkWriteInChannelWithLog(channel)) {
                message = channel.sendMessage(getMessageEmbed()).get();
                if (channel.canYouAddNewReactions()) {
                    message.addReaction(emoji);
                }
                return Optional.of(message);
            } else return Optional.empty();
        } else {
            editMessage.edit(getMessageEmbed()).exceptionally(ExceptionLogger.get());
            return Optional.of(editMessage);
        }
    }

    private EmbedBuilder getMessageEmbed() {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(getEmoji() + " " + title)
                .setDescription(description)
                .setFooter(getString("endson"))
                .setTimestamp(Instant.now().plus(durationMinutes, ChronoUnit.MINUTES));

        eb.addField(Emojis.EMPTY_EMOJI, getString("tutorial", amountOfWinners != 1, emoji.getMentionTag(), String.valueOf(amountOfWinners)));

        if (imageLink != null)
            eb.setImage(imageLink);
        return eb;
    }

}
