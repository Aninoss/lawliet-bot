package commands.runnables.utilitycategory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import commands.*;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnAlertListener;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "alerts",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🔔",
        executableWithoutArgs = true,
        usesExtEmotes = true,
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
    public boolean onTrigger(CommandEvent event, String args) {
        serverId = event.getGuild().getIdLong();
        channelId = event.getChannel().getIdLong();
        alerts = DBTracker.getInstance().retrieve(event.getGuild().getIdLong());
        patreon = PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong());

        controll(args, event.getMember());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state != STATE_REMOVE) {
            cont = true;
            return controll(input, event.getMember());
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
                        deregisterListenersWithComponentMessage();
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

            controll(key, event.getMember());
            return true;
        }

        return false;
    }

    private MessageInputResponse controll(String searchTerm, Member member) {
        while (true) {
            if (searchTerm.replace(" ", "").isEmpty()) {
                return MessageInputResponse.SUCCESS;
            }

            String arg = searchTerm.split(" ")[0].toLowerCase();
            MessageInputResponse currentMessageInputResponse = processArg(arg, searchTerm, member);
            if (currentMessageInputResponse != MessageInputResponse.SUCCESS || !cont) {
                return currentMessageInputResponse;
            }

            searchTerm = searchTerm.substring(arg.length()).trim();
        }
    }

    private MessageInputResponse processArg(String arg, String argComplete, Member member) {
        int state = getState();
        return switch (state) {
            case DEFAULT_STATE -> processMain(arg);
            case STATE_ADD -> processAdd(arg);
            case STATE_REMOVE -> processRemove(arg);
            case STATE_KEY -> processKey(argComplete);
            case STATE_USERMESSAGE -> processUserMessage(argComplete, member);
            default -> null;
        };
    }

    private MessageInputResponse processMain(String arg) {
        switch (arg) {
            case "add":
                if (enoughSpaceForNewTrackers()) {
                    setState(STATE_ADD);
                    return MessageInputResponse.SUCCESS;
                } else {
                    return MessageInputResponse.FAILED;
                }

            case "remove":
                if (getTrackersInChannel().size() > 0) {
                    setState(STATE_REMOVE);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, getString("notracker"));
                    return MessageInputResponse.FAILED;
                }

            default:
                return null;
        }
    }

    private MessageInputResponse processAdd(String arg) {
        if (!enoughSpaceForNewTrackers()) {
            return null;
        }

        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(arg, getLocale(), getPrefix());
        if (commandOpt.isEmpty() || !(commandOpt.get() instanceof OnAlertListener) || !commandOpt.get().canRunOnGuild(0L, 0L)) {
            return null;
        }

        Command command = commandOpt.get();
        if (command.getCommandProperties().nsfw() && !ShardManager.getLocalGuildById(serverId).get().getTextChannelById(channelId).isNSFW()) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_block_description"));
            return MessageInputResponse.FAILED;
        }

        if (command.getCommandProperties().patreonRequired() &&
                !PatreonCache.getInstance().hasPremium(getMemberId().get(), true) &&
                !PatreonCache.getInstance().isUnlocked(getGuildId().get())
        ) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
            return MessageInputResponse.FAILED;
        }

        if (trackerSlotExists(command.getTrigger(), "")) {
            setLog(LogStatus.FAILURE, getString("state1_alreadytracking", command.getTrigger()));
            return MessageInputResponse.FAILED;
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
        return MessageInputResponse.SUCCESS;
    }

    private MessageInputResponse processRemove(String arg) {
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

        return MessageInputResponse.FAILED;
    }

    private MessageInputResponse processKey(String arg) {
        if (!enoughSpaceForNewTrackers()) {
            return MessageInputResponse.FAILED;
        }

        if (arg.length() > LIMIT_KEY_LENGTH) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", String.valueOf(LIMIT_KEY_LENGTH)));
            return MessageInputResponse.FAILED;
        }

        if (trackerSlotExists(commandCache.getTrigger(), arg)) {
            setLog(LogStatus.FAILURE, getString("state3_alreadytracking", arg));
            return MessageInputResponse.FAILED;
        }

        commandKeyCache = arg;
        setState(STATE_USERMESSAGE);
        cont = false;
        return MessageInputResponse.SUCCESS;
    }

    private MessageInputResponse processUserMessage(String args, Member member) {
        cont = false;
        if (args.equals("no")) {
            addTracker(null);
            return MessageInputResponse.SUCCESS;
        } else {
            if (PatreonCache.getInstance().isUnlocked(getGuildId().get()) ||
                    PatreonCache.getInstance().hasPremium(getMemberId().get(), true)
            ) {
                if (!BotPermissionUtil.memberCanMentionRoles(getTextChannel().get(), member, args)) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "user_nomention"));
                    return MessageInputResponse.FAILED;
                }

                addTracker(args);
                return MessageInputResponse.SUCCESS;
            } else {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                return MessageInputResponse.FAILED;
            }
        }
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) throws Throwable {
        setComponents(getString("state0_options").split("\n"));

        buttonMap.clear();
        buttonMap.put(-1, "back");
        buttonMap.put(0, "add");
        buttonMap.put(1, "remove");

        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd(Member member) throws Throwable {
        buttonMap.clear();
        buttonMap.put(-1, "back");

        List<Command> trackerCommands = getAllTrackerCommands();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

        for (Category category : Category.independentValues()) {
            StringBuilder sb = new StringBuilder();
            trackerCommands.stream()
                    .filter(command -> command.getCategory().equals(category))
                    .forEach(command -> {
                        sb.append(getString("slot_add", command.getTrigger(),
                                command.getCommandProperties().patreonRequired() ? Emojis.COMMAND_ICON_PREMIUM : ""
                        )).append("\n");
                    });

            if (sb.length() > 0) {
                eb.addField(TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId()), sb.toString(), true);
            }
        }

        String commandInfo = TextManager.getString(getLocale(), Category.INFORMATION, "help_commandproperties_PATREON", Emojis.COMMAND_ICON_PREMIUM, ExternalLinks.PREMIUM_WEBSITE);
        eb.addField(Emojis.ZERO_WIDTH_SPACE, commandInfo, false);

        return eb;
    }

    @Draw(state = STATE_REMOVE)
    public EmbedBuilder onDrawRemove(Member member) throws Throwable {
        buttonMap.clear();
        buttonMap.put(-1, "back");

        List<TrackerData> trackerData = getTrackersInChannel();
        String[] options = new String[trackerData.size()];

        for (int i = 0; i < options.length; i++) {
            String trigger = trackerData.get(i).getCommandTrigger();

            options[i] = getString("slot_remove", trackerData.get(i).getCommandKey().length() > 0,
                    trigger,
                    StringUtil.escapeMarkdown(StringUtil.shortenString(trackerData.get(i).getCommandKey(), 200))
            );
            buttonMap.put(i, String.valueOf(i));
        }

        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_KEY)
    public EmbedBuilder onDrawKey(Member member) {
        buttonMap.clear();
        buttonMap.put(-1, "back");
        return EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), commandCache.getCategory(), commandCache.getTrigger() + "_trackerkey"), getString("state3_title"));
    }

    @Draw(state = STATE_USERMESSAGE)
    public EmbedBuilder onDrawUserMessage(Member member) {
        buttonMap.clear();
        buttonMap.put(-1, "back");
        buttonMap.put(0, "no");
        setComponents(getString("state4_options").split("\n"));

        return EmbedFactory.getEmbedDefault(
                this,
                getString("state4_description", ExternalLinks.PREMIUM_WEBSITE),
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
                userMessage,
                Instant.now()
        );

        alerts.put(slot.hashCode(), slot);
        AlertScheduler.loadAlert(slot);
        setState(STATE_ADD);
        setLog(LogStatus.SUCCESS, getString("state3_added", commandCache.getTrigger()));
    }

    private List<Command> getAllTrackerCommands() {
        return CommandContainer.getTrackerCommands().stream()
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
