package commands.runnables.utilitycategory;

import java.time.Instant;
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
import core.TextManager;
import core.atomicassets.AtomicBaseGuildMessageChannel;
import core.cache.ServerPatreonBoostCache;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.schedulers.AlertScheduler;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

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
            STATE_COMMAND = 3,
            STATE_KEY = 4,
            STATE_USERMESSAGE = 5;

    public static final int LIMIT_CHANNEL = 5;
    public static final int LIMIT_SERVER = 20;
    public static final int LIMIT_KEY_LENGTH = 500;

    private long channelId = 0L;
    private CustomObservableMap<Integer, TrackerData> alerts;
    private Command commandCache;
    private String commandKeyCache;

    public AlertsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        alerts = DBTracker.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = STATE_ADD)
    public MessageInputResponse onMessageAdd(MessageReceivedEvent event, String input) {
        List<BaseGuildMessageChannel> channelList = MentionUtil.getBaseGuildMessageChannels(event.getGuild(), input).getList();
        if (channelList.size() > 0) {
            BaseGuildMessageChannel channel = channelList.get(0);
            channelId = channel.getIdLong();
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = STATE_COMMAND)
    public MessageInputResponse onMessageCommand(MessageReceivedEvent event, String input) {
        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(input.toLowerCase(), getLocale(), getPrefix());
        if (commandOpt.isEmpty() || !(commandOpt.get() instanceof OnAlertListener) || !commandOpt.get().canRunOnGuild(0L, 0L)) {
            return null;
        }

        BaseGuildMessageChannel channel = getAlertChannelOrFail(event.getMember());
        if (channel == null) {
            return MessageInputResponse.FAILED;
        }

        Command command = commandOpt.get();
        if (command.getCommandProperties().nsfw() && !channel.isNSFW()) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_block_description"));
            return MessageInputResponse.FAILED;
        }

        if (command.getCommandProperties().patreonRequired() &&
                !ServerPatreonBoostCache.get(event.getGuild().getIdLong())
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
            setState(STATE_USERMESSAGE);
        }
        return MessageInputResponse.SUCCESS;
    }

    @ControllerMessage(state = STATE_KEY)
    public MessageInputResponse onMessageKey(MessageReceivedEvent event, String input) {
        if (getAlertChannelOrFail(event.getMember()) == null) {
            return MessageInputResponse.FAILED;
        }

        if (input.length() > LIMIT_KEY_LENGTH) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", String.valueOf(LIMIT_KEY_LENGTH)));
            return MessageInputResponse.FAILED;
        }

        if (trackerSlotExists(commandCache.getTrigger(), input)) {
            setLog(LogStatus.FAILURE, getString("state3_alreadytracking", input));
            return MessageInputResponse.FAILED;
        }

        commandKeyCache = input;
        setState(STATE_USERMESSAGE);
        return MessageInputResponse.SUCCESS;
    }

    @ControllerMessage(state = STATE_USERMESSAGE)
    public MessageInputResponse onMessageUserMessage(MessageReceivedEvent event, String input) {
        BaseGuildMessageChannel channel = getAlertChannelOrFail(event.getMember());
        if (channel == null) {
            return MessageInputResponse.FAILED;
        }

        if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
            if (!BotPermissionUtil.memberCanMentionRoles(channel, event.getMember(), input)) {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "user_nomention"));
                return MessageInputResponse.FAILED;
            }

            addTracker(event.getGuild().getIdLong(), input);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                channelId = event.getChannel().getIdLong();
                if (enoughSpaceForNewTrackers(event.getMember())) {
                    setState(STATE_ADD);
                }
                return true;

            case 1:
                if (alerts.size() > 0) {
                    setState(STATE_REMOVE);
                } else {
                    setLog(LogStatus.FAILURE, getString("notracker"));
                }
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_ADD)
    public boolean onButtonAdd(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        } else if (i == 0) {
            BaseGuildMessageChannel channel = event.getGuild().getChannelById(BaseGuildMessageChannel.class, channelId);
            if (channel != null) {
                if (BotPermissionUtil.canWriteEmbed(channel)) {
                    setState(STATE_COMMAND);
                } else {
                    String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", "#" + channel.getName());
                    setLog(LogStatus.FAILURE, error);
                }
                return true;
            } else {
                setLog(LogStatus.FAILURE, getString("invalidchannel"));
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = STATE_REMOVE)
    public boolean onButtonRemove(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        } else {
            TrackerData slotRemove = alerts.get(Integer.parseInt(event.getComponentId()));
            if (slotRemove != null) {
                slotRemove.delete();
                setLog(LogStatus.SUCCESS, getString("state2_removed", slotRemove.getCommandTrigger()));
                if (alerts.size() == 0) {
                    setState(DEFAULT_STATE);
                }
            }

            return true;
        }
    }

    @ControllerButton(state = STATE_COMMAND)
    public boolean onButtonCommand(ButtonInteractionEvent event, int i) {
        if (getAlertChannelOrFail(event.getMember()) == null) {
            return true;
        }

        if (i == -1) {
            setState(STATE_ADD);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_KEY)
    public boolean onButtonKey(ButtonInteractionEvent event, int i) {
        if (getAlertChannelOrFail(event.getMember()) == null) {
            return true;
        }

        if (i == -1) {
            setState(STATE_COMMAND);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_USERMESSAGE)
    public boolean onButtonUserMessage(ButtonInteractionEvent event, int i) {
        if (getAlertChannelOrFail(event.getMember()) == null) {
            return true;
        }

        if (i == -1) {
            setState(STATE_COMMAND);
            return true;
        } else if (i == 0) {
            addTracker(event.getGuild().getIdLong(), null);
            return true;
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) throws Throwable {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd(Member member) throws Throwable {
        setComponents(TextManager.getString(getLocale(), TextManager.GENERAL, "continue"));
        AtomicBaseGuildMessageChannel atomicChannel = new AtomicBaseGuildMessageChannel(member.getGuild().getIdLong(), channelId);
        return EmbedFactory.getEmbedDefault(this, getString("state5_description", atomicChannel.getAsMention()), getString("state5_title"));
    }

    @Draw(state = STATE_REMOVE)
    public EmbedBuilder onDrawRemove(Member member) throws Throwable {
        List<Button> buttons = alerts.values().stream()
                .sorted((a0, a1) -> {
                    long channelO = a0.getBaseGuildMessageChannelId();
                    long channel1 = a1.getBaseGuildMessageChannelId();
                    if (channelO == channel1) {
                        return a0.getCreationTime().compareTo(a1.getCreationTime());
                    } else {
                        return Long.compare(channelO, channel1);
                    }
                })
                .map(alert -> {
                    String trigger = alert.getCommandTrigger();
                    String channelName = StringUtil.escapeMarkdown(StringUtil.shortenString(new AtomicBaseGuildMessageChannel(member.getGuild().getIdLong(), alert.getBaseGuildMessageChannelId()).getPrefixedName(), 40));
                    String label  = getString("slot_remove", false, channelName, trigger);
                    return Button.of(ButtonStyle.PRIMARY, String.valueOf(alert.hashCode()), label);
                })
                .collect(Collectors.toList());

        setComponents(buttons);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_COMMAND)
    public EmbedBuilder onDrawCommand(Member member) throws Throwable {
        List<Command> trackerCommands = getAllTrackerCommands();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

        for (Category category : Category.independentValues()) {
            StringBuilder sb = new StringBuilder();
            trackerCommands.stream()
                    .filter(command -> command.getCategory().equals(category))
                    .forEach(command -> {
                        sb.append(getString("slot_add", command.getTrigger(),
                                command.getCommandProperties().patreonRequired() ? Emojis.COMMAND_ICON_PREMIUM.getFormatted() : ""
                        )).append("\n");
                    });

            if (sb.length() > 0) {
                eb.addField(TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId()), sb.toString(), true);
            }
        }

        String commandInfo = TextManager.getString(getLocale(), Category.INFORMATION, "help_commandproperties_PATREON", Emojis.COMMAND_ICON_PREMIUM.getFormatted(), ExternalLinks.PREMIUM_WEBSITE);
        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), commandInfo, false);

        return eb;
    }

    @Draw(state = STATE_KEY)
    public EmbedBuilder onDrawKey(Member member) {
        return EmbedFactory.getEmbedDefault(this, TextManager.getString(getLocale(), commandCache.getCategory(), commandCache.getTrigger() + "_trackerkey"), getString("state3_title"));
    }

    @Draw(state = STATE_USERMESSAGE)
    public EmbedBuilder onDrawUserMessage(Member member) {
        setComponents(getString("state4_options").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state4_description", ExternalLinks.PREMIUM_WEBSITE),
                getString("state4_title")
        );
    }

    private void addTracker(long guildId, String userMessage) {
        TrackerData slot = new TrackerData(
                guildId,
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
        setState(STATE_COMMAND);
        setLog(LogStatus.SUCCESS, getString("state3_added", commandCache.getTrigger()));
    }

    private List<Command> getAllTrackerCommands() {
        return CommandContainer.getTrackerCommands().stream()
                .map(clazz -> CommandManager.createCommandByClass((Class<? extends Command>) clazz, getLocale(), getPrefix()))
                .collect(Collectors.toList());
    }

    private boolean trackerSlotExists(String commandTrigger, String commandKey) {
        return alerts.values().stream()
                .anyMatch(slot -> slot.getCommandTrigger().equals(commandTrigger) &&
                        slot.getBaseGuildMessageChannelId() == channelId &&
                        slot.getCommandKey().equalsIgnoreCase(commandKey)
                );
    }

    private boolean enoughSpaceForNewTrackers(Member member) {
        boolean premium = ServerPatreonBoostCache.get(member.getGuild().getIdLong());
        if (channelId == 0L || alerts.values().stream().filter(a -> a.getBaseGuildMessageChannelId() == channelId).count() < LIMIT_CHANNEL || premium) {
            if (alerts.size() < LIMIT_SERVER || premium) {
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

    private BaseGuildMessageChannel getAlertChannelOrFail(Member member) {
        BaseGuildMessageChannel channel = member.getGuild().getChannelById(BaseGuildMessageChannel.class, channelId);
        if (channel == null || !enoughSpaceForNewTrackers(member)) {
            setState(STATE_ADD);
            return null;
        } else {
            return channel;
        }
    }

}
