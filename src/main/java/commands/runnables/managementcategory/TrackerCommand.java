package commands.runnables.managementcategory;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import commands.listeners.OnTrackerRequestListener;
import constants.*;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.TextManager;
import core.emojiconnection.BackEmojiConnection;
import core.emojiconnection.EmojiConnection;
import core.utils.StringUtil;
import mysql.modules.server.DBServer;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerBean;
import mysql.modules.tracker.TrackerBeanSlot;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "alerts",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "🔔",
        executable = true,
        aliases = { "tracker", "track", "tracking", "alert", "auto", "automate" }
)
public class TrackerCommand extends Command implements OnNavigationListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(TrackerCommand.class);

    private final int
            STATE_ADD = 1,
            STATE_REMOVE = 2,
            STATE_KEY = 3,
            STATE_SUCCESS = 4;

    private final int LIMIT_CHANNEL = 10;
    private final int LIMIT_SERVER = 30;
    private final int LIMIT_KEY_LENGTH = 500;

    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private long serverId;
    private long channelId;
    private TrackerBean trackerBean;
    private Command commandCache;

    public TrackerCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        serverId = event.getServer().get().getId();
        channelId = event.getServerTextChannel().get().getId();
        trackerBean = DBTracker.getInstance().getBean();
        controll(followedString, true);
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state != STATE_REMOVE) {
            controll(inputString, false);
            return Response.TRUE;
        }
        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        for (EmojiConnection emojiConnection: emojiConnections) {
            if (emojiConnection.isEmoji(event.getEmoji()) || (i == -1 && emojiConnection instanceof BackEmojiConnection)) {
                if (emojiConnection.getConnection().equalsIgnoreCase("back")) {
                    switch (state) {
                        case 0:
                            removeNavigationWithMessage();
                            return false;

                        case 1:
                            setState(DEFAULT_STATE);
                            return true;

                        case 2:
                            setState(DEFAULT_STATE);
                            return true;

                        case 3:
                            setState(STATE_ADD);
                            return true;

                        default:
                    }
                }

                controll(emojiConnection.getConnection(), false);
                return true;
            }
        }

        return false;
    }

    private void controll(String searchTerm, boolean firstTime) throws Throwable {
        while(true) {
            if (searchTerm.replace(" ", "").isEmpty()) return;
            String arg = searchTerm.split(" ")[0].toLowerCase();

            if (!processArg(arg, searchTerm, firstTime)) return;
            searchTerm = StringUtil.trimString(searchTerm.substring(arg.length()));
        }
    }

    private boolean processArg(String arg, String argComplete, boolean firstTime) throws ExecutionException, IllegalAccessException, InstantiationException, InvocationTargetException {
        int state = getState();
        switch (state) {
            case DEFAULT_STATE:
                return processMain(arg);

            case STATE_ADD:
                return processAdd(arg, firstTime);

            case STATE_REMOVE:
                return processRemove(arg, firstTime);

            case STATE_KEY:
                return processKey(argComplete, firstTime);

            default:
                return false;
        }
    }

    private boolean processMain(String arg) {
        switch (arg) {
            case "add":
                if (enoughSpaceForNewTrackers()) {
                    setState(STATE_ADD);
                    return true;
                } else {
                    return false;
                }

            case "remove":
                if (getTrackersInChannel().size() > 0) {
                    setState(STATE_REMOVE);
                    return true;
                } else {
                    setLog(LogStatus.FAILURE, getString("notracker"));
                    return false;
                }

            default:
                return false;
        }
    }

    private boolean processAdd(String arg, boolean firstTime) throws ExecutionException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (!enoughSpaceForNewTrackers())
            return false;

        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(arg, getLocale(), getPrefix());
        if (commandOpt.isEmpty() || !(commandOpt.get() instanceof OnTrackerRequestListener)) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", arg));
            return false;
        }

        Command command = commandOpt.get();
        if (command.isNsfw() && !DiscordApiCollection.getInstance().getServerById(serverId).get().getTextChannelById(channelId).get().isNsfw()) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_block_description"));
            return false;
        }

        if (trackerSlotExists(command.getTrigger(), "")) {
            setLog(LogStatus.FAILURE, getString("state1_alreadytracking", command.getTrigger()));
            return false;
        }

        OnTrackerRequestListener trackerCommand = (OnTrackerRequestListener)command;
        if (trackerCommand.trackerUsesKey()) {
            commandCache = command;
            setState(STATE_KEY);
            return true;
        } else {
            addTracker(command, "", firstTime);
            return false;
        }
    }

    private boolean processRemove(String arg, boolean firstTime) throws ExecutionException {
        List<TrackerBeanSlot> trackerSlots = getTrackersInChannel();

        if (!StringUtil.stringIsInt(arg))
            return false;

        int index = Integer.parseInt(arg);
        if (index < 0 || index >= trackerSlots.size())
            return false;

        TrackerBeanSlot slotRemove = trackerSlots.get(index);
        slotRemove.delete();
        setLog(LogStatus.SUCCESS, getString("state2_removed", slotRemove.getCommandTrigger()));
        if (getTrackersInChannel().size() == 0) {
            setState(0);
        }

        return false;
    }

    private boolean processKey(String arg, boolean firstTime) throws ExecutionException {
        if (!enoughSpaceForNewTrackers())
            return false;

        if (arg.length() > LIMIT_KEY_LENGTH) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", String.valueOf(LIMIT_KEY_LENGTH)));
            return false;
        }

        if (trackerSlotExists(commandCache.getTrigger(), arg)) {
            setLog(LogStatus.FAILURE, getString("state3_alreadytracking", arg));
            return false;
        }

        addTracker(commandCache, arg, firstTime);
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(DiscordApi api) throws Throwable {
        ServerTextChannel channel = getStarterMessage().getServerTextChannel().get();
        setOptions(getString("state0_options").split("\n"));

        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(channel, "back"));
        emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[0], "add"));
        emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[1], "remove"));

        return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"));
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd(DiscordApi api) throws Throwable {
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getStarterMessage().getServerTextChannel().get(), "back"));

        List<Command> trackerCommands = getAllTrackerCommands();

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

        for(String category : Category.LIST) {
            StringBuilder sb = new StringBuilder();
            trackerCommands.stream()
                    .filter(command -> command.getCategory().equals(category))
                    .forEach(command -> {
                        String nsfwEmoji = DiscordApiCollection.getInstance().getHomeEmojiById(652188472295292998L).getMentionTag();
                        sb.append(getString("slot_add", command.isNsfw(), command.getTrigger(), nsfwEmoji))
                                .append("\n");
                    });

            if (sb.length() > 0)
                eb.addField(TextManager.getString(getLocale(), TextManager.COMMANDS, category), sb.toString(), true);
        }

        return eb;
    }

    @Draw(state = STATE_REMOVE)
    public EmbedBuilder onDrawRemove(DiscordApi api) throws Throwable {
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getStarterMessage().getServerTextChannel().get(), "back"));

        List<TrackerBeanSlot> trackerSlots = getTrackersInChannel();
        setOptions(new String[trackerSlots.size()]);

        for (int i = 0; i < getOptions().length; i++) {
            Command command = CommandManager.createCommandByTrigger(trackerSlots.get(i).getCommandTrigger(), getLocale(), getPrefix()).get();
            String trigger = command.getTrigger();

            getOptions()[i] = getString("slot_remove", trackerSlots.get(i).getCommandKey().length() > 0,
                    trigger,
                    StringUtil.escapeMarkdown(StringUtil.shortenString(trackerSlots.get(i).getCommandKey(), 200))
            );
            emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], String.valueOf(i)));
        }

        return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_KEY)
    public EmbedBuilder onDrawKey(DiscordApi api) throws Throwable {
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getStarterMessage().getServerTextChannel().get(), "back"));
        return EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), commandCache.getCategory(),  commandCache.getTrigger() + "_trackerkey"), getString("state3_title"));
    }

    @Draw(state = STATE_SUCCESS)
    public EmbedBuilder onDrawSuccess(DiscordApi api) throws Throwable {
        removeNavigation();
        return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"));
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return LIMIT_CHANNEL;
    }

    private void addTracker(Command command, String commandKey, boolean firstTime) throws ExecutionException {
        TrackerBeanSlot slot = new TrackerBeanSlot(
                DBServer.getInstance().getBean(serverId),
                channelId,
                command.getTrigger(),
                null,
                commandKey,
                Instant.now(),
                null
        );
        trackerBean.getSlots().add(slot);
        if (firstTime) {
            setState(STATE_SUCCESS);
        } else {
            setState(STATE_ADD);
            setLog(LogStatus.SUCCESS, getString("state3_added", command.getTrigger()));
        }
    }

    private List<Command> getAllTrackerCommands() {
        return CommandContainer.getInstance().getTrackerCommands().stream()
                .map(clazz -> {
                    try {
                        return CommandManager.createCommandByClass((Class<? extends Command>)clazz, getLocale(), getPrefix());
                    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                        LOGGER.error("Error while creating command class", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean trackerSlotExists(String commandTrigger, String commandKey) {
        return getTrackersInChannel().stream()
                .anyMatch(slot -> slot.getCommandTrigger().equals(commandTrigger) && slot.getCommandKey().equalsIgnoreCase(commandKey));
    }

    private boolean enoughSpaceForNewTrackers() {
        if (getTrackersInChannel().size() < LIMIT_CHANNEL) {
            if (getTrackersInServer().size() < LIMIT_SERVER) {
                return true;
            } else {
                setLog(LogStatus.FAILURE, getString("toomuch_server", String.valueOf(LIMIT_SERVER)));
                return false;
            }
        } else {
            setLog(LogStatus.FAILURE, getString("toomuch_channel", String.valueOf(LIMIT_CHANNEL)));
            return false;
        }
    }

    private List<TrackerBeanSlot> getTrackersInChannel() {
        return trackerBean.getSlots().stream()
                .filter(slot -> slot.getChannelId() == channelId)
                .collect(Collectors.toList());
    }

    private List<TrackerBeanSlot> getTrackersInServer() {
        return trackerBean.getSlots().stream()
                .filter(slot -> slot.getServerId() == serverId)
                .collect(Collectors.toList());
    }

}
