package commands.runnables.utilitycategory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.runnables.NavigationAbstract;
import constants.*;
import core.CustomObservableMap;
import core.EmbedFactory;
import core.ShardManager;
import core.TextManager;
import core.cache.PatreonCache;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import modules.schedulers.AlertScheduler;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "alerts",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI, //TODO: remove
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🔔",
        executableWithoutArgs = true,
        aliases = { "tracker", "track", "tracking", "alert", "auto", "automate", "automize", "feed", "feeds" }
)
public class AlertsCommand extends NavigationAbstract {

    private final int
            STATE_ADD = 1,
            STATE_REMOVE = 2,
            STATE_KEY = 3,
            STATE_USERMESSAGE = 4;

    private final int LIMIT_CHANNEL = 5;
    private final int LIMIT_SERVER = 20;
    private final int LIMIT_KEY_LENGTH = 500;

    private final HashMap<Integer, String> buttonMap = new HashMap<>();
    private long serverId;
    private long channelId;
    private boolean patreon;
    private CustomObservableMap<Integer, TrackerData> alerts;
    private Command commandCache;
    private String commandKeyCache;
    private boolean cont = true;

    public AlertsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        serverId = event.getGuild().getIdLong();
        channelId = event.getChannel().getIdLong();
        alerts = DBTracker.getInstance().retrieve(event.getGuild().getIdLong());
        patreon = PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) >= 3 ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

        controll(args);
        registerNavigationListener();
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state != STATE_REMOVE) {
            cont = true;
            return controll(input);
        }
        return null;
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) {
        String key = buttonMap.get(i);
        if (key != null) {
            if (key.equalsIgnoreCase("back")) {
                switch (state) {
                    case DEFAULT_STATE:
                        deregisterListenersWithButtonMessage();
                        return false;

                    case STATE_ADD:
                    case STATE_REMOVE:
                        setState(DEFAULT_STATE);
                        return true;

                    case STATE_KEY:
                    case STATE_USERMESSAGE:
                        setState(STATE_ADD);
                        return true;

                    default:
                        return false;
                }
            }

            controll(key);
            return true;
        }

        return false;
    }

    private Response controll(String searchTerm) {
        while (true) {
            if (searchTerm.replace(" ", "").isEmpty()) {
                return Response.TRUE;
            }

            String arg = searchTerm.split(" ")[0].toLowerCase();
            Response currentResponse = processArg(arg, searchTerm);
            if (currentResponse != Response.TRUE || !cont) {
                return currentResponse;
            }

            searchTerm = searchTerm.substring(arg.length()).trim();
        }
    }

    private Response processArg(String arg, String argComplete) {
        int state = getState();
        switch (state) {
            case DEFAULT_STATE:
                return processMain(arg);

            case STATE_ADD:
                return processAdd(arg);

            case STATE_REMOVE:
                return processRemove(arg);

            case STATE_KEY:
                return processKey(argComplete);

            case STATE_USERMESSAGE:
                return processUserMessage(argComplete);

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

    private Response processAdd(String arg) {
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
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_block_description_short"));
            return Response.FALSE;
        }

        if (command.getCommandProperties().patreonRequired() &&
                PatreonCache.getInstance().getUserTier(getMemberId().get(), true) < 2 &&
                !PatreonCache.getInstance().isUnlocked(getGuildId().get())
        ) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
            return Response.FALSE;
        }

        if (trackerSlotExists(command.getTrigger(), "")) {
            setLog(LogStatus.FAILURE, getString("state1_alreadytracking", command.getTrigger()));
            return Response.FALSE;
        }

        OnAlertListener trackerCommand = (OnAlertListener) command;
        commandCache = command;
        if (trackerCommand.trackerUsesKey()) {
            setState(STATE_KEY);
        } else {
            commandKeyCache = "";
            cont = false;
            setState(STATE_USERMESSAGE);
        }
        return Response.TRUE;
    }

    private Response processRemove(String arg) {
        List<TrackerData> trackerData = getTrackersInChannel();

        if (!StringUtil.stringIsInt(arg)) {
            return null;
        }

        int index = Integer.parseInt(arg) + 10 * getPage();
        if (index < 0 || index >= trackerData.size()) {
            return null;
        }

        TrackerData slotRemove = trackerData.get(index);
        slotRemove.delete();
        setLog(LogStatus.SUCCESS, getString("state2_removed", slotRemove.getCommandTrigger()));
        if (getTrackersInChannel().size() == 0) {
            setState(DEFAULT_STATE);
        }

        return Response.FALSE;
    }

    private Response processKey(String arg) {
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

        commandKeyCache = arg;
        setState(STATE_USERMESSAGE);
        cont = false;
        return Response.TRUE;
    }

    private Response processUserMessage(String args) {
        cont = false;
        if (args.equals("no")) {
            addTracker(null);
            return Response.TRUE;
        } else {
            if (PatreonCache.getInstance().isUnlocked(getGuildId().get()) ||
                    PatreonCache.getInstance().getUserTier(getMemberId().get(), true) >= 3
            ) {
                if (!BotPermissionUtil.memberCanMentionRoles(getTextChannel().get(), getMember().get(), args)) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "user_nomention"));
                    return Response.FALSE;
                }

                addTracker(args);
                return Response.TRUE;
            } else {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                return Response.FALSE;
            }
        }
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain() throws Throwable {
        setOptions(getString("state0_options").split("\n"));

        buttonMap.clear();
        buttonMap.put(-1, "back");
        buttonMap.put(0, "add");
        buttonMap.put(1, "remove");

        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd() throws Throwable {
        buttonMap.clear();
        buttonMap.put(-1, "back");

        List<Command> trackerCommands = getAllTrackerCommands();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

        for (String category : Category.LIST) {
            StringBuilder sb = new StringBuilder();
            trackerCommands.stream()
                    .filter(command -> command.getCategory().equals(category))
                    .forEach(command -> {
                        sb.append(getString("slot_add", command.getTrigger(),
                                command.getCommandProperties().nsfw() ? Emojis.COMMAND_ICON_NSFW : "",
                                command.getCommandProperties().patreonRequired() ? Emojis.COMMAND_ICON_PATREON : ""
                        )).append("\n");
                    });

            if (sb.length() > 0) {
                eb.addField(TextManager.getString(getLocale(), TextManager.COMMANDS, category), sb.toString(), true);
            }
        }

        String commandInfo = TextManager.getString(getLocale(), Category.INFORMATION, "help_commandproperties_NSFW", Emojis.COMMAND_ICON_NSFW) + "\n" +
                TextManager.getString(getLocale(), Category.INFORMATION, "help_commandproperties_PATREON", Emojis.COMMAND_ICON_PATREON, ExternalLinks.PATREON_PAGE);
        eb.addField(Emojis.ZERO_WIDTH_SPACE, commandInfo, false);

        return eb;
    }

    @Draw(state = STATE_REMOVE)
    public EmbedBuilder onDrawRemove() throws Throwable {
        buttonMap.clear();
        buttonMap.put(-1, "back");

        List<TrackerData> trackerData = getTrackersInChannel();
        setOptions(new String[trackerData.size()]);

        for (int i = 0; i < getOptions().length; i++) {
            String trigger = trackerData.get(i).getCommandTrigger();

            getOptions()[i] = getString("slot_remove", trackerData.get(i).getCommandKey().length() > 0,
                    trigger,
                    StringUtil.escapeMarkdown(StringUtil.shortenString(trackerData.get(i).getCommandKey(), 200))
            );
            buttonMap.put(i, String.valueOf(i));
        }

        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_KEY)
    public EmbedBuilder onDrawKey() {
        buttonMap.clear();
        buttonMap.put(-1, "back");
        return EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), commandCache.getCategory(), commandCache.getTrigger() + "_trackerkey"), getString("state3_title"));
    }

    @Draw(state = STATE_USERMESSAGE)
    public EmbedBuilder onDrawUserMessage() {
        buttonMap.clear();
        buttonMap.put(-1, "back");
        buttonMap.put(0, "no");
        setOptions(getString("state4_options").split("\n"));

        return EmbedFactory.getEmbedDefault(
                this,
                getString("state4_description", ExternalLinks.PATREON_PAGE, ExternalLinks.UNLOCK_SERVER_WEBSITE),
                getString("state4_title")
        );
    }

    private void addTracker(String userMessage) {
        TrackerData slot = new TrackerData(
                serverId,
                channelId,
                commandCache.getTrigger(),
                null,
                commandKeyCache,
                Instant.now(),
                null,
                null,
                userMessage
        );

        alerts.put(slot.hashCode(), slot);
        AlertScheduler.getInstance().loadAlert(slot);
        setState(STATE_ADD);
        setLog(LogStatus.SUCCESS, getString("state3_added", commandCache.getTrigger()));
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
        if (getTrackersInChannel().size() < LIMIT_CHANNEL || patreon) {
            if (alerts.size() < LIMIT_SERVER || patreon) {
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

    private List<TrackerData> getTrackersInChannel() {
        return alerts.values().stream()
                .filter(slot -> slot != null && slot.getTextChannelId() == channelId)
                .collect(Collectors.toList());
    }

}
