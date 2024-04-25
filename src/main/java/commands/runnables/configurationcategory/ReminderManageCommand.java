package commands.runnables.configurationcategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.runnables.utilitycategory.ReminderCommand;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.atomicassets.AtomicGuildMessageChannel;
import core.cache.ServerPatreonBoostCache;
import core.modals.DurationModalBuilder;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.ReminderEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "remindermanage",
        emoji = "⏲️",
        executableWithoutArgs = true,
        aliases = {"reminderconfig"}
)
public class ReminderManageCommand extends NavigationAbstract {

    public static int MAX_REPEATING_DM_REMINDERS = 3;
    public static int REPEATING_DM_REMINDERS_MIN_INTERVAL_MINUTES = 60;

    private static final int STATE_DM_REMINDERS = 1,
            STATE_GUILD_REMINDERS = 2,
            STATE_MANAGE = 3,
            STATE_SET_MESSAGE = 4,
            STATE_SET_CHANNEL = 5;

    private UUID id;
    private AtomicGuildMessageChannel guildChannel;
    private Instant triggerTime;
    private String message;
    private Integer intervalMinutes;
    private ReminderEntity.Type type;

    public ReminderManageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember(), List.of(
                new StringStateProcessor(this, STATE_SET_MESSAGE, STATE_MANAGE, getString("manage_message"))
                        .setClearButton(false)
                        .setMax(ReminderCommand.MESSAGE_CONTENT_MAX_LENGTH)
                        .setGetter(() -> this.message)
                        .setSetter(input -> this.message = input),
                new GuildChannelsStateProcessor(this, STATE_SET_CHANNEL, STATE_MANAGE, getString("manage_channel"))
                        .setMinMax(1, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
                        .setSingleGetter(() -> guildChannel != null ? guildChannel.getIdLong() : null)
                        .setSingleSetter(channelId -> this.guildChannel = new AtomicGuildMessageChannel(event.getGuild().getIdLong(), channelId))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                if (getUserEntity().getReminders().isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("err_nodm"));
                    return true;
                }
                setState(STATE_DM_REMINDERS);
                return true;
            }
            case 1 -> {
                if (!BotPermissionUtil.can(event.getMember(), Permission.MANAGE_SERVER)) {
                    setLog(LogStatus.FAILURE, getString("err_missingpermissions"));
                    return true;
                }
                if (getGuildEntity().getReminders().isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("err_noguild"));
                    return true;
                }
                setState(STATE_GUILD_REMINDERS);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = STATE_DM_REMINDERS)
    public boolean onButtonDm(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        UUID uuid = UUID.fromString(event.getComponentId());
        ReminderEntity reminderEntity = getEntityManager().find(ReminderEntity.class, uuid);
        if (reminderEntity == null) {
            return true;
        }

        id = reminderEntity.getId();
        guildChannel = reminderEntity.getGuildChannelId() != null ? new AtomicGuildMessageChannel(event.getGuild().getIdLong(), reminderEntity.getGuildChannelId()) : null;
        triggerTime = reminderEntity.getTriggerTime();
        message = reminderEntity.getMessage();
        intervalMinutes = reminderEntity.getIntervalMinutes();
        type = reminderEntity.getType();
        setState(STATE_MANAGE);
        return true;
    }

    @ControllerButton(state = STATE_GUILD_REMINDERS)
    public boolean onButtonGuild(ButtonInteractionEvent event, int i) {
        return onButtonDm(event, i);
    }

    @ControllerButton(state = STATE_MANAGE)
    public boolean onButtonManage(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setStateReminders();
                return true;
            }
            case 0 -> {
                setState(STATE_SET_CHANNEL);
            }
            case 1 -> {
                if (type == ReminderEntity.Type.GUILD_REMINDER && intervalMinutes == null && !ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, getString("err_nopremium"));
                    return true;
                }
                if (type == ReminderEntity.Type.DM_REMINDER && intervalMinutes == null && getNumberOfRepeatingDmReminders(id) + 1 > MAX_REPEATING_DM_REMINDERS) {
                    setLog(LogStatus.FAILURE, getString("err_toomanydmreminders", StringUtil.numToString(MAX_REPEATING_DM_REMINDERS)));
                    return true;
                }

                Modal modal = new DurationModalBuilder(this, getString("manage_interval"))
                        .setMinMinutes(0)
                        .setGetter(() -> intervalMinutes != null ? intervalMinutes.longValue() : null)
                        .setSetterOptionalLogs(intervalMinutes -> {
                            if (intervalMinutes == null) {
                                this.intervalMinutes = null;
                                setLog(LogStatus.SUCCESS, getString("set_interval"));
                                return false;
                            }

                            if (type == ReminderEntity.Type.GUILD_REMINDER) {
                                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                                    setLog(LogStatus.FAILURE, getString("err_nopremium"));
                                    return false;
                                }
                            } else {
                                if (getNumberOfRepeatingDmReminders(id) + 1 > MAX_REPEATING_DM_REMINDERS) {
                                    setLog(LogStatus.FAILURE, getString("err_toomanydmreminders", StringUtil.numToString(MAX_REPEATING_DM_REMINDERS)));
                                    return false;
                                }
                                if (intervalMinutes < REPEATING_DM_REMINDERS_MIN_INTERVAL_MINUTES) {
                                    setLog(LogStatus.FAILURE, getString("err_dmrepetitiontooshort", StringUtil.numToString(REPEATING_DM_REMINDERS_MIN_INTERVAL_MINUTES)));
                                    return false;
                                }
                            }

                            this.intervalMinutes = intervalMinutes.intValue();
                            setLog(LogStatus.SUCCESS, getString("set_interval"));
                            return false;
                        })
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                setState(STATE_SET_MESSAGE);
                return true;
            }
            case 3 -> {
                GuildMessageChannel channel = null;
                if (guildChannel != null) {
                    channel = guildChannel.get().orElse(null);
                    if (channel == null) {
                        setLog(LogStatus.FAILURE, getString("err_missingchannel"));
                        return true;
                    }

                    if (!BotPermissionUtil.canWriteEmbed(channel)) {
                        String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel", "#" + StringUtil.escapeMarkdownInField(channel.getName()));
                        setLog(LogStatus.FAILURE, error);
                        return true;
                    }

                    if (!BotPermissionUtil.memberCanMentionRoles(channel, event.getMember(), message)) {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "user_nomention"));
                        return true;
                    }
                }

                if (type == ReminderEntity.Type.GUILD_REMINDER && intervalMinutes != null && !ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, getString("err_nopremium"));
                    return true;
                }
                if (type == ReminderEntity.Type.DM_REMINDER && intervalMinutes != null && getNumberOfRepeatingDmReminders(id) + 1 > MAX_REPEATING_DM_REMINDERS) {
                    setLog(LogStatus.FAILURE, getString("err_toomanydmreminders", StringUtil.numToString(MAX_REPEATING_DM_REMINDERS)));
                    return true;
                }

                EntityManagerWrapper entityManager = getEntityManager();
                ReminderEntity reminderEntity = entityManager.find(ReminderEntity.class, id);
                if (reminderEntity == null) {
                    setLog(LogStatus.FAILURE, getString("err_noreminder"));
                    setStateReminders();
                    return true;
                }

                entityManager.getTransaction().begin();
                if (type == ReminderEntity.Type.GUILD_REMINDER) {
                    BotLogEntity.log(entityManager, BotLogEntity.Event.REMINDERS_EDIT, event.getMember(), "\"" + StringUtil.shortenString(reminderEntity.getMessage(), 40) + "\"");
                    reminderEntity.setGuildChannelId(guildChannel.getIdLong());
                }
                reminderEntity.setMessage(message);
                reminderEntity.setIntervalMinutes(intervalMinutes);
                entityManager.getTransaction().commit();

                EmbedBuilder eb = ReminderCommand.generateEmbed(
                        getLocale(),
                        getPrefix(),
                        channel,
                        reminderEntity.getTriggerTime(),
                        reminderEntity.getMessage(),
                        reminderEntity.getIntervalMinutesEffectively()
                );
                reminderEntity.editConfirmationMessage(event.getJDA(), eb.build());

                setLog(LogStatus.SUCCESS, getString("set"));
                setStateReminders();
                return true;
            }
            case 4 -> {
                EntityManagerWrapper entityManager = getEntityManager();
                ReminderEntity reminderEntity = entityManager.find(ReminderEntity.class, id);

                if (reminderEntity != null) {
                    entityManager.getTransaction().begin();
                    if (type == ReminderEntity.Type.GUILD_REMINDER) {
                        BotLogEntity.log(entityManager, BotLogEntity.Event.REMINDERS_DELETE, event.getMember(), "\"" + StringUtil.shortenString(reminderEntity.getMessage(), 40) + "\"");
                    }
                    entityManager.remove(reminderEntity);
                    entityManager.getTransaction().commit();

                    reminderEntity.deleteConfirmationMessage(event.getJDA());
                }

                setLog(LogStatus.SUCCESS, getString("set_remove"));
                setStateReminders();
                return true;
            }
        }

        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawDefault(Member member) {
        setComponents(getString("default_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("default_desc"));
    }

    @Draw(state = STATE_DM_REMINDERS)
    public EmbedBuilder onDrawDm(Member member) {
        List<Button> buttons = getUserEntity().getReminders().stream()
                .map(reminderEntity -> Button.of(ButtonStyle.PRIMARY, reminderEntity.getId().toString(), "\"" + StringUtil.shortenString(reminderEntity.getMessage(), 40) + "\""))
                .collect(Collectors.toList());
        setComponents(buttons);
        return EmbedFactory.getEmbedDefault(this, getString("list_desc"), getString("dm_title"));
    }

    @Draw(state = STATE_GUILD_REMINDERS)
    public EmbedBuilder onDrawGuild(Member member) {
        List<Button> buttons = getGuildEntity().getReminders().stream()
                .map(reminderEntity -> Button.of(ButtonStyle.PRIMARY, reminderEntity.getId().toString(), "\"" + StringUtil.shortenString(reminderEntity.getMessage(), 40) + "\""))
                .collect(Collectors.toList());
        setComponents(buttons);
        return EmbedFactory.getEmbedDefault(this, getString("list_desc"), getString("guild_title"));
    }

    @Draw(state = STATE_MANAGE)
    public EmbedBuilder onDrawManage(Member member) {
        String[] options = getString("manage_options").split("\n");
        if (type == ReminderEntity.Type.DM_REMINDER) {
            options[0] = "";
        }
        setComponents(options, Set.of(3), Set.of(4));

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, null,
                getString(type == ReminderEntity.Type.DM_REMINDER ? "manage_dm_title" : "manage_guild_title")
        );

        if (type == ReminderEntity.Type.GUILD_REMINDER) {
            eb.addField(getString("manage_channel"), guildChannel.getPrefixedNameInField(getLocale()), true);
        }
        eb.addField(getString("manage_triggertime"), TimeFormat.DATE_TIME_SHORT.atInstant(triggerTime).toString(), true);

        String intervalText = TextManager.getString(getLocale(), Category.UTILITY, "reminder_norep");
        if (intervalMinutes != null && intervalMinutes > 0) {
            intervalText = TimeUtil.getDurationString(getLocale(), Duration.ofMinutes(intervalMinutes));
        }

        String add = type == ReminderEntity.Type.GUILD_REMINDER ? " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted() : "";
        eb.addField(getString("manage_interval") + add, intervalText, true)
                .addField(getString("manage_message"), StringUtil.shortenString(message, 1024), false);
        return eb;
    }

    private void setStateReminders() {
        if (type == ReminderEntity.Type.DM_REMINDER) {
            if (getUserEntity().getReminders().isEmpty()) {
                setState(DEFAULT_STATE);
            } else {
                setState(STATE_DM_REMINDERS);
            }
        } else {
            if (getGuildEntity().getReminders().isEmpty()) {
                setState(DEFAULT_STATE);
            } else {
                setState(STATE_GUILD_REMINDERS);
            }
        }
    }

    private int getNumberOfRepeatingDmReminders(UUID ignoreId) {
        return (int) getUserEntity().getReminders().stream()
                .filter(reminder -> reminder.getIntervalMinutesEffectively() != null && !Objects.equals(reminder.getId(), ignoreId))
                .count();
    }

}
