package commands.runnables.utilitycategory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.runnables.NavigationAbstract;
import constants.Category;
import constants.Emojis;
import constants.LogStatus;
import constants.Response;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.ShardManager;
import core.TextManager;
import core.cache.PatreonCache;
import core.emojiconnection.BackEmojiConnection;
import core.emojiconnection.EmojiConnection;
import core.utils.StringUtil;
import modules.schedulers.AlertScheduler;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "alerts",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🔔",
        executableWithoutArgs = true,
        aliases = { "tracker", "track", "tracking", "alert", "auto", "automate", "automize", "feed", "feeds" }
)
public class AlertsCommand extends NavigationAbstract {

    private final int
            STATE_ADD = 1,
            STATE_REMOVE = 2,
            STATE_KEY = 3;

    private final int LIMIT_CHANNEL = 5;
    private final int LIMIT_SERVER = 20;
    private final int LIMIT_KEY_LENGTH = 500;

    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private long serverId;
    private long channelId;
    private int patreonLevel;
    private CustomObservableMap<Integer, TrackerSlot> alerts;
    private Command commandCache;
    private boolean cont = true;
    private boolean addNavigation = true;

    public AlertsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        serverId = event.getGuild().getIdLong();
        channelId = event.getChannel().getIdLong();
        alerts = DBTracker.getInstance().retrieve(event.getGuild().getIdLong());
        patreonLevel = PatreonCache.getInstance().getUserTier(event.getMember().getIdLong());
        controll(args, true);
        if (addNavigation) {
            registerNavigationListener(12);
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("state3_added", commandCache.getTrigger()));
            drawMessage(eb);
        }
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state != STATE_REMOVE) {
            return controll(input, false);
        }
        return null;
    }

    @Override
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) {
        for (EmojiConnection emojiConnection : emojiConnections) {
            if (emojiConnection.isEmoji(event.getReactionEmote()) || (i == -1 && emojiConnection instanceof BackEmojiConnection)) {
                if (emojiConnection.getConnection().equalsIgnoreCase("back")) {
                    switch (state) {
                        case 0:
                            removeNavigationWithMessage();
                            return false;

                        case 1:

                        case 2:
                        case 3:
                            setState(DEFAULT_STATE);
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

    private Response controll(String searchTerm, boolean firstTime) {
        while (true) {
            if (searchTerm.replace(" ", "").isEmpty()) {
                return Response.TRUE;
            }
            String arg = searchTerm.split(" ")[0].toLowerCase();

            Response currentResponse = processArg(arg, searchTerm, firstTime);

            if (currentResponse == Response.FALSE) return Response.FALSE;
            if (currentResponse == null) return null;
            if (!cont) return currentResponse;

            searchTerm = searchTerm.substring(arg.length()).trim();
        }
    }

    private Response processArg(String arg, String argComplete, boolean firstTime) {
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

    private Response processAdd(String arg, boolean firstTime) {
        if (!enoughSpaceForNewTrackers()) {
            return null;
        }

        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(arg, getLocale(), getPrefix());
        if (commandOpt.isEmpty() || !(commandOpt.get() instanceof OnAlertListener)) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), arg));
            return null;
        }

        Command command = commandOpt.get();
        if (command.getCommandProperties().nsfw() && !ShardManager.getInstance().getLocalGuildById(serverId).get().getTextChannelById(channelId).isNSFW()) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_block_description"));
            return Response.FALSE;
        }

        if (trackerSlotExists(command.getTrigger(), "")) {
            setLog(LogStatus.FAILURE, getString("state1_alreadytracking", command.getTrigger()));
            return Response.FALSE;
        }

        OnAlertListener trackerCommand = (OnAlertListener) command;
        if (trackerCommand.trackerUsesKey()) {
            commandCache = command;
            setState(STATE_KEY);
        } else {
            addTracker(command, "", firstTime);
        }
        return Response.TRUE;
    }

    private Response processRemove(String arg) {
        List<TrackerSlot> trackerSlots = getTrackersInChannel();

        if (!StringUtil.stringIsInt(arg)) {
            return null;
        }

        int index = Integer.parseInt(arg) + 10 * getPage();
        if (index < 0 || index >= trackerSlots.size()) {
            return null;
        }

        TrackerSlot slotRemove = trackerSlots.get(index);
        slotRemove.delete();
        setLog(LogStatus.SUCCESS, getString("state2_removed", slotRemove.getCommandTrigger()));
        if (getTrackersInChannel().size() == 0) {
            setState(0);
        }

        return Response.FALSE;
    }

    private Response processKey(String arg, boolean firstTime) {
        if (!enoughSpaceForNewTrackers()) {
            return Response.FALSE;
        }

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
    public EmbedBuilder onDrawMain() throws Throwable {
        setOptions(getString("state0_options").split("\n"));

        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getTextChannel().get(), "back"));
        emojiConnections.add(new EmojiConnection(Emojis.LETTERS[0], "add"));
        emojiConnections.add(new EmojiConnection(Emojis.LETTERS[1], "remove"));

        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd() throws Throwable {
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getTextChannel().get(), "back"));

        List<Command> trackerCommands = getAllTrackerCommands();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

        for (String category : Category.LIST) {
            StringBuilder sb = new StringBuilder();
            trackerCommands.stream()
                    .filter(command -> command.getCategory().equals(category))
                    .forEach(command -> {
                        sb.append(getString("slot_add", command.getCommandProperties().nsfw(), command.getTrigger(), Emojis.COMMAND_ICON_NSFW))
                                .append("\n");
                    });

            if (sb.length() > 0) {
                eb.addField(TextManager.getString(getLocale(), TextManager.COMMANDS, category), sb.toString(), true);
            }
        }

        return eb;
    }

    @Draw(state = STATE_REMOVE)
    public EmbedBuilder onDrawRemove() throws Throwable {
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getTextChannel().get(), "back"));

        List<TrackerSlot> trackerSlots = getTrackersInChannel();
        setOptions(new String[trackerSlots.size()]);

        for (int i = 0; i < getOptions().length; i++) {
            String trigger = trackerSlots.get(i).getCommandTrigger();

            getOptions()[i] = getString("slot_remove", trackerSlots.get(i).getCommandKey().length() > 0,
                    trigger,
                    StringUtil.escapeMarkdown(StringUtil.shortenString(trackerSlots.get(i).getCommandKey(), 200))
            );
            emojiConnections.add(new EmojiConnection(Emojis.LETTERS[i], String.valueOf(i)));
        }

        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_KEY)
    public EmbedBuilder onDrawKey() throws Throwable {
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(getTextChannel().get(), "back"));
        return EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), commandCache.getCategory(), commandCache.getTrigger() + "_trackerkey"), getString("state3_title"));
    }

    private void addTracker(Command command, String commandKey, boolean firstTime) {
        TrackerSlot slot = new TrackerSlot(
                serverId,
                channelId,
                command.getTrigger(),
                null,
                commandKey,
                Instant.now(),
                null,
                null
        );

        alerts.put(slot.hashCode(), slot);
        AlertScheduler.getInstance().loadAlert(slot);

        if (firstTime) {
            commandCache = command;
            addNavigation = false;
        } else {
            setState(STATE_ADD);
            setLog(LogStatus.SUCCESS, getString("state3_added", command.getTrigger()));
        }
    }

    private List<Command> getAllTrackerCommands() {
        return CommandContainer.getInstance().getTrackerCommands().stream()
                .map(clazz -> CommandManager.createCommandByClass((Class<? extends Command>) clazz, getLocale(), getPrefix()))
                .collect(Collectors.toList());
    }

    private boolean trackerSlotExists(String commandTrigger, String commandKey) {
        return getTrackersInChannel().stream()
                .anyMatch(slot -> slot.getCommandTrigger().equals(commandTrigger) && slot.getCommandKey().equalsIgnoreCase(commandKey));
    }

    private boolean enoughSpaceForNewTrackers() {
        if (getTrackersInChannel().size() < LIMIT_CHANNEL || patreonLevel >= 3) {
            if (alerts.size() < LIMIT_SERVER || patreonLevel >= 3) {
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

    private List<TrackerSlot> getTrackersInChannel() {
        return alerts.values().stream()
                .filter(slot -> slot != null && slot.getTextChannelId() == channelId)
                .collect(Collectors.toList());
    }

}
