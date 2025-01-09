package commands.runnables.configurationcategory;

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
import core.atomicassets.AtomicGuildMessageChannel;
import core.cache.ServerPatreonBoostCache;
import core.modals.DurationModalBuilder;
import core.modals.StringModalBuilder;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.schedulers.AlertScheduler;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.tracker.DBTracker;
import mysql.modules.tracker.TrackerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "alerts",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🔔",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"tracker", "track", "tracking", "alert", "auto", "automate", "automize", "feed", "feeds"}
)
public class AlertsCommand extends NavigationAbstract {

    private final int
            STATE_ADD = 1,
            STATE_REMOVE = 2,
            STATE_COMMAND = 3,
            STATE_KEY = 4,
            STATE_USERMESSAGE = 5,
            STATE_MININTERVAL = 6;

    public static final int LIMIT_CHANNEL = 5;
    public static final int LIMIT_SERVER = 20;
    public static final int LIMIT_KEY_LENGTH = 500;

    private long channelId = 0L;
    private CustomObservableMap<Integer, TrackerData> alerts;
    private Command commandCache;
    private String commandKeyCache;
    private String userMessage;

    public AlertsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        alerts = DBTracker.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = STATE_USERMESSAGE)
    public MessageInputResponse onMessageUserMessage(MessageReceivedEvent event, String input) {
        GuildMessageChannel channel = getAlertChannelOrFail(event.getMember());
        if (channel == null) {
            return MessageInputResponse.FAILED;
        }

        if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
            if (!BotPermissionUtil.memberCanMentionRoles(channel, event.getMember(), input)) {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "user_nomention"));
                return MessageInputResponse.FAILED;
            }

            userMessage = input;
            setState(STATE_MININTERVAL);
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
                if (!alerts.isEmpty()) {
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
                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.ALERTS, event.getMember(), null, slotRemove.getCommandTrigger());
                getEntityManager().getTransaction().commit();

                slotRemove.delete();
                setLog(LogStatus.SUCCESS, getString("state2_removed", slotRemove.getCommandTrigger()));
                if (alerts.isEmpty()) {
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
        } else if (i == 0) {
            Modal modal = new StringModalBuilder(this, getString("dashboard_command"), TextInputStyle.SHORT)
                    .setMinMaxLength(1, 50)
                    .setSetterOptionalLogs(input -> {
                        String prefix = getPrefix();
                        if (input.toLowerCase().startsWith(prefix.toLowerCase())) {
                            input = input.substring(prefix.length());
                        }

                        Optional<Command> commandOpt = CommandManager.createCommandByTrigger(input.toLowerCase(), getLocale(), getPrefix());
                        if (commandOpt.isEmpty() || !(commandOpt.get() instanceof OnAlertListener) || !commandOpt.get().canRunOnGuild(0L, 0L)) {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
                            return false;
                        }

                        GuildMessageChannel channel = getAlertChannelOrFail(event.getMember());
                        if (channel == null) {
                            return false;
                        }

                        Command command = commandOpt.get();
                        if (command.getCommandProperties().nsfw() && !JDAUtil.channelIsNsfw(channel)) {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "nsfw_block_description", getPrefix()).replace("`", "\""));
                            return false;
                        }

                        if (command.getCommandProperties().patreonRequired() &&
                                !ServerPatreonBoostCache.get(event.getGuild().getIdLong())
                        ) {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                            return false;
                        }

                        if (trackerSlotExists(command.getTrigger(), "")) {
                            setLog(LogStatus.FAILURE, getString("state1_alreadytracking", command.getTrigger()));
                            return false;
                        }

                        OnAlertListener trackerCommand = (OnAlertListener) command;
                        commandCache = command;
                        if (trackerCommand.trackerUsesKey()) {
                            setState(STATE_KEY);
                        } else {
                            commandKeyCache = "";
                            setState(STATE_USERMESSAGE);
                        }
                        return false;
                    })
                    .build();
            event.replyModal(modal).queue();
            return false;
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
        } else if (i == 0) {
            Modal modal = new StringModalBuilder(this, getString("dashboard_arg"), TextInputStyle.SHORT)
                    .setMinMaxLength(1, TextInput.MAX_VALUE_LENGTH)
                    .setSetterOptionalLogs(input -> {
                        if (getAlertChannelOrFail(event.getMember()) == null) {
                            return false;
                        }

                        if (input.length() > LIMIT_KEY_LENGTH) {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", String.valueOf(LIMIT_KEY_LENGTH)));
                            return false;
                        }

                        if (trackerSlotExists(commandCache.getTrigger(), input)) {
                            setLog(LogStatus.FAILURE, getString("state3_alreadytracking", input));
                            return false;
                        }

                        commandKeyCache = input;
                        setState(STATE_USERMESSAGE);
                        return false;
                    })
                    .build();
            event.replyModal(modal).queue();
            return false;
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
            userMessage = null;
            setState(STATE_MININTERVAL);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_MININTERVAL)
    public boolean onButtonMinInterval(ButtonInteractionEvent event, int i) {
        if (getAlertChannelOrFail(event.getMember()) == null) {
            return true;
        }

        switch (i) {
            case -1 -> {
                setState(STATE_USERMESSAGE);
                return true;
            }
            case 0 -> {
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                    return true;
                }

                Modal modal = new DurationModalBuilder(this, getString("dashboard_mininterval"))
                        .setMinMinutes(1)
                        .setSetterOptionalLogs(minutes -> {
                            GuildMessageChannel channel = getAlertChannelOrFail(event.getMember());
                            if (channel == null) {
                                return false;
                            }

                            if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                                addTracker(event.getMember(), minutes.intValue());
                            } else {
                                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                            }
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                addTracker(event.getMember(), 0);
                return true;
            }
        }
        return false;
    }

    @ControllerEntitySelectMenu(state = STATE_ADD)
    public boolean onSelectMenuAddMessage(EntitySelectInteractionEvent event) {
        GuildMessageChannel channel = (GuildMessageChannel) event.getMentions().getChannels().get(0);
        if (checkWriteEmbedInChannelWithLog(channel)) {
            this.channelId = channel.getIdLong();
            setState(STATE_COMMAND);
        }
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) throws Throwable {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = STATE_ADD)
    public EmbedBuilder onDrawAdd(Member member) throws Throwable {
        EntitySelectMenu entitySelectMenu = EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                .setRequiredRange(1, 1)
                .build();

        setComponents(entitySelectMenu);
        return EmbedFactory.getEmbedDefault(this, StringUtil.stepPoints(0, 5) + "\n\n" + getString("state5_description"), getString("state5_title"));
    }

    @Draw(state = STATE_REMOVE)
    public EmbedBuilder onDrawRemove(Member member) throws Throwable {
        List<Button> buttons = alerts.values().stream()
                .sorted((a0, a1) -> {
                    long channelO = a0.getGuildMessageChannelId();
                    long channel1 = a1.getGuildMessageChannelId();
                    if (channelO == channel1) {
                        return a0.getCreationTime().compareTo(a1.getCreationTime());
                    } else {
                        return Long.compare(channelO, channel1);
                    }
                })
                .map(alert -> {
                    String trigger = alert.getCommandTrigger();
                    String channelName = StringUtil.shortenString(StringUtil.escapeMarkdown(new AtomicGuildMessageChannel(member.getGuild().getIdLong(), alert.getGuildMessageChannelId()).getPrefixedName(getLocale())), 40);
                    String label = getString("slot_remove", false, channelName, trigger) + " ✕";
                    return Button.of(ButtonStyle.PRIMARY, String.valueOf(alert.hashCode()), label);
                })
                .collect(Collectors.toList());

        setComponents(buttons);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_COMMAND)
    public EmbedBuilder onDrawCommand(Member member) {
        setComponents(getString("state1_options").split("\n"));

        List<Command> trackerCommands = getAllTrackerCommands();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, StringUtil.stepPoints(1, 5) + "\n\n" + getString("state1_description"), getString("state1_title"));

        for (Category category : Category.independentValues()) {
            StringBuilder sb = new StringBuilder();
            trackerCommands.stream()
                    .filter(command -> command.getCategory().equals(category))
                    .forEach(command -> {
                        String add = "";
                        if (command.getCommandProperties().patreonRequired()) {
                            add = Emojis.COMMAND_ICON_PREMIUM.getFormatted();
                        } else if (command.getReleaseDate().orElse(LocalDate.now()).isAfter(LocalDate.now())) {
                            add = getString("beta");
                        }

                        sb.append(getString("slot_add", command.getTrigger(), add))
                                .append("\n");
                    });

            if (!sb.isEmpty()) {
                EmbedUtil.addFieldSplit(eb, TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId()), sb.toString(), true);
            }
        }

        String commandInfo = TextManager.getString(getLocale(), Category.INFORMATION, "help_commandproperties_PATREON", Emojis.COMMAND_ICON_PREMIUM.getFormatted(), ExternalLinks.PREMIUM_WEBSITE);
        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), commandInfo, false);

        return eb;
    }

    @Draw(state = STATE_KEY)
    public EmbedBuilder onDrawKey(Member member) {
        setComponents(getString("state3_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, StringUtil.stepPoints(2, 5) + "\n\n" + TextManager.getString(getLocale(), commandCache.getCategory(), commandCache.getTrigger() + "_trackerkey"), getString("state3_title"));
    }

    @Draw(state = STATE_USERMESSAGE)
    public EmbedBuilder onDrawUserMessage(Member member) {
        setComponents(getString("state4_options").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                StringUtil.stepPoints(3, 5) + "\n\n" + getString("state4_description", ExternalLinks.PREMIUM_WEBSITE),
                getString("state4_title")
        );
    }

    @Draw(state = STATE_MININTERVAL)
    public EmbedBuilder onDrawMinInterval(Member member) {
        setComponents(getString("state6_options").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                StringUtil.stepPoints(4, 5) + "\n\n" + getString("state6_description", ExternalLinks.PREMIUM_WEBSITE),
                getString("state6_title")
        );
    }

    private void addTracker(Member member, int minInterval) {
        TrackerData slot = new TrackerData(
                member.getGuild().getIdLong(),
                channelId,
                commandCache.getTrigger(),
                null,
                commandKeyCache,
                Instant.now(),
                null,
                null,
                userMessage,
                Instant.now(),
                minInterval
        );

        getEntityManager().getTransaction().begin();
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.ALERTS, member, commandCache.getTrigger(), null);
        getEntityManager().getTransaction().commit();

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
                        slot.getGuildMessageChannelId() == channelId &&
                        slot.getCommandKey().equalsIgnoreCase(commandKey)
                );
    }

    private boolean enoughSpaceForNewTrackers(Member member) {
        boolean premium = ServerPatreonBoostCache.get(member.getGuild().getIdLong());

        if (alerts.size() >= LIMIT_SERVER && !premium) {
            setLog(LogStatus.FAILURE, getString("toomuch_server", String.valueOf(LIMIT_SERVER)));
            return false;
        }

        if (channelId != 0L &&
                alerts.values().stream().filter(a -> a.getGuildMessageChannelId() == channelId).count() >= LIMIT_CHANNEL &&
                !premium) {
            setLog(LogStatus.FAILURE, getString("toomuch_channel", String.valueOf(LIMIT_CHANNEL)));
            return false;
        }

        return true;
    }

    private GuildMessageChannel getAlertChannelOrFail(Member member) {
        GuildMessageChannel channel = member.getGuild().getChannelById(GuildMessageChannel.class, channelId);
        if (channel == null || !enoughSpaceForNewTrackers(member)) {
            setState(STATE_ADD);
            return null;
        } else {
            return channel;
        }
    }

}
