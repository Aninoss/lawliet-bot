package commands.runnables.utilitycategory;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnTrackerRequestListener;
import constants.*;
import core.EmbedFactory;
import core.MainLogger;
import core.ShardManager;
import core.TextManager;
import core.cache.PatreonCache;
import core.emojiconnection.BackEmojiConnection;
import core.emojiconnection.EmojiConnection;
import core.utils.StringUtil;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerBean;
import mysql.modules.tracker.TrackerBeanSlot;
import net.dv8tion.jda.api.EmbedBuilder;

@CommandProperties(
        trigger = "alerts",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "🔔",
        executableWithoutArgs = true,
        aliases = { "tracker", "track", "tracking", "alert", "auto", "automate", "automize", "feed", "feeds" }
)
public class AlertsCommand extends Command implements OnNavigationListenerOld {

    private final int
            STATE_ADD = 1,
            STATE_REMOVE = 2,
            STATE_KEY = 3,
            STATE_SUCCESS = 4;

    private final int LIMIT_CHANNEL = 5;
    private final int LIMIT_SERVER = 20;
    private final int LIMIT_KEY_LENGTH = 500;

    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private long serverId;
    private long channelId;
    private int patreonLevel;
    private TrackerBean trackerBean;
    private Command commandCache;
    private boolean cont = true;

    public AlertsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String args) throws Throwable {
        serverId = event.getGuild().getIdLong();
        channelId = event.getServerTextChannel().get().getId();
        trackerBean = DBTracker.getInstance().retrieve();
        patreonLevel = PatreonCache.getInstance().getUserTier(event.getMessageAuthor().getId());
        controll(args, true);
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state != STATE_REMOVE) {
            return controll(inputString, false);
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

    private Response controll(String searchTerm, boolean firstTime) throws Throwable {
        while(true) {
            if (searchTerm.replace(" ", "").isEmpty())
                return Response.TRUE;
            String arg = searchTerm.split(" ")[0].toLowerCase();

            Response currentResponse = processArg(arg, searchTerm, firstTime);

            if (currentResponse == Response.FALSE) return Response.FALSE;
            if (currentResponse == null) return null;
            if (!cont) return currentResponse;

            searchTerm = searchTerm.substring(arg.length()).trim();
        }
    }

    private Response processArg(String arg, String argComplete, boolean firstTime) throws ExecutionException, IllegalAccessException, InstantiationException, InvocationTargetException {
        int state = getState();
        switch (state) {
            case DEFAULT_STATE:
                return processMain(arg);

            case STATE_ADD:
                return processAdd(arg, firstTime);

            case STATE_REMOVE:
                return processRemove(arg);

            case STATE_KEY:
                return processKey(argComplete, firstTime);

            default:
                return null;
        }
    }

    private Response processMain(String arg) {
        switch (arg) {
            case "add":
                if (enoughSpaceForNewTrackers()) {
                    setState(STATE_ADD);
                    return Response.TRUE;
                } else {
                    return Response.FALSE;
                }

            case "remove":
                if (getTrackersInChannel().size() > 0) {
                    setState(STATE_REMOVE);
                    return Response.TRUE;
                } else {
                    setLog(LogStatus.FAILURE, getString("notracker"));
                    return Response.FALSE;
                }

            default:
                return null;
        }
    }

    private Response processAdd(String arg, boolean firstTime) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (!enoughSpaceForNewTrackers())
            return null;

        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(arg, getLocale(), getPrefix());
        if (commandOpt.isEmpty() || !(commandOpt.get() instanceof OnTrackerRequestListener)) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), arg));
            return null;
        }

        Command command = commandOpt.get();
        if (command.isNsfw() && !ShardManager.getInstance().getLocalGuildById(serverId).get().getTextChannelById(channelId).get().isNsfw()) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_block_description"));
            return Response.FALSE;
        }

        if (trackerSlotExists(command.getTrigger(), "")) {
            setLog(LogStatus.FAILURE, getString("state1_alreadytracking", command.getTrigger()));
            return Response.FALSE;
        }

        OnTrackerRequestListener trackerCommand = (OnTrackerRequestListener)command;
        if (trackerCommand.trackerUsesKey()) {
            commandCache = command;
            setState(STATE_KEY);
            return Response.TRUE;
        } else {
            addTracker(command, "", firstTime);
            return Response.TRUE;
        }
    }

    private Response processRemove(String arg) {
        List<TrackerBeanSlot> trackerSlots = getTrackersInChannel();

        if (!StringUtil.stringIsInt(arg))
            return null;

        int index = Integer.parseInt(arg) + 10 * getPage();
        if (index < 0 || index >= trackerSlots.size())
            return null;

        TrackerBeanSlot slotRemove = trackerSlots.get(index);
        slotRemove.delete();
        setLog(LogStatus.SUCCESS, getString("state2_removed", slotRemove.getCommandTrigger()));
        if (getTrackersInChannel().size() == 0) {
            setState(0);
        }

        return Response.FALSE;
    }

    private Response processKey(String arg, boolean firstTime) {
        if (!enoughSpaceForNewTrackers())
            return Response.FALSE;

        if (arg.length() > LIMIT_KEY_LENGTH) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", String.valueOf(LIMIT_KEY_LENGTH)));
            return Response.FALSE;
        }

        if (trackerSlotExists(commandCache.getTrigger(), arg)) {
            setLog(LogStatus.FAILURE, getString("state3_alreadytracking", arg));
            return Response.FALSE;
        }

        addTracker(commandCache, arg, firstTime);
        cont = false;
        return Response.TRUE;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(DiscordApi api) throws Throwable {
        ServerTextChannel channel = getStarterMessage().getServerTextChannel().get();
        setOptions(getString("state0_options").split("\n"));

        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(channel, "back"));
        emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[0], "add"));
        emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[1], "remove"));

        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd(DiscordApi api) throws Throwable {
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getStarterMessage().getServerTextChannel().get(), "back"));

        List<Command> trackerCommands = getAllTrackerCommands();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

        for(String category : Category.LIST) {
            StringBuilder sb = new StringBuilder();
            trackerCommands.stream()
                    .filter(command -> command.getCategory().equals(category))
                    .forEach(command -> {
                        sb.append(getString("slot_add", command.isNsfw(), command.getTrigger(), Emojis.COMMAND_ICON_NSFW))
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
            String trigger = trackerSlots.get(i).getCommandTrigger();

            getOptions()[i] = getString("slot_remove", trackerSlots.get(i).getCommandKey().length() > 0,
                    trigger,
                    StringUtil.escapeMarkdown(StringUtil.shortenString(trackerSlots.get(i).getCommandKey(), 200))
            );
            emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], String.valueOf(i)));
        }

        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_KEY)
    public EmbedBuilder onDrawKey(DiscordApi api) throws Throwable {
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getStarterMessage().getServerTextChannel().get(), "back"));
        return EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), commandCache.getCategory(),  commandCache.getTrigger() + "_trackerkey"), getString("state3_title"));
    }

    @Draw(state = STATE_SUCCESS)
    public EmbedBuilder onDrawSuccess(DiscordApi api) throws Throwable {
        removeNavigation();
        return EmbedFactory.getEmbedDefault(this, getString("state3_added", commandCache.getTrigger()));
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    private void addTracker(Command command, String commandKey, boolean firstTime) {
        TrackerBeanSlot slot = new TrackerBeanSlot(
                serverId,
                channelId,
                command.getTrigger(),
                null,
                commandKey,
                Instant.now(),
                null
        );
        trackerBean.getSlots().add(slot);
        if (firstTime) {
            commandCache = command;
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
                        MainLogger.get().error("Error while creating command class", e);
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
        if (getTrackersInChannel().size() < LIMIT_CHANNEL || patreonLevel >= 3) {
            if (getTrackersInServer().size() < LIMIT_SERVER || patreonLevel >= 3) {
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
        return new ArrayList<>(trackerBean.getSlots()).stream()
                .filter(slot -> slot != null && slot.getChannelId() == channelId)
                .collect(Collectors.toList());
    }

    private List<TrackerBeanSlot> getTrackersInServer() {
        return new ArrayList<>(trackerBean.getSlots()).stream()
                .filter(slot -> slot.getGuildId() == serverId)
                .collect(Collectors.toList());
    }

}
