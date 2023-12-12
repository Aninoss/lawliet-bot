package commands.runnables.configurationcategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.runnables.utilitycategory.ReminderCommand;
import constants.Emojis;
import constants.ExceptionIds;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicStandardGuildMessageChannel;
import core.cache.ServerPatreonBoostCache;
import core.modals.ModalMediator;
import core.utils.BotPermissionUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.ReminderEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
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

    private static final int STATE_DM_REMINDERS = 1,
            STATE_GUILD_REMINDERS = 2,
            STATE_MANAGE = 3,
            STATE_SET_CHANNEL = 4;

    private UUID id;
    private AtomicStandardGuildMessageChannel guildChannel;
    private Instant triggerTime;
    private String message;
    private Integer intervalMinutes;
    private ReminderEntity.Type type;

    public ReminderManageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
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
        guildChannel = reminderEntity.getGuildChannelId() != null ? new AtomicStandardGuildMessageChannel(event.getGuild(), reminderEntity.getGuildChannelId()) : null;
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
                if (intervalMinutes == null && !ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, getString("err_nopremium"));
                    return true;
                }

                String textId = "interval";
                TextInput textInput = TextInput.create(textId, getString("manage_interval_label"), TextInputStyle.SHORT)
                        .setMinLength(0)
                        .setMaxLength(12)
                        .setRequired(false)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("manage_interval_title"), e -> {
                            String interval = e.getValue(textId).getAsString();
                            if (interval.isEmpty()) {
                                this.intervalMinutes = null;
                                setLog(LogStatus.SUCCESS, getString("set_interval"));
                                return null;
                            }

                            int intervalMinutes = MentionUtil.getTimeMinutes(interval).getValue().intValue();
                            if (intervalMinutes == 0L) {
                                setLog(LogStatus.FAILURE, StringUtil.escapeMarkdownInField(getString("err_invalidtime", interval)));
                                return null;
                            }

                            if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                                setLog(LogStatus.FAILURE, getString("err_nopremium"));
                                return null;
                            }

                            this.intervalMinutes = intervalMinutes;
                            setLog(LogStatus.SUCCESS, getString("set_interval"));
                            return null;
                        }).addActionRows(ActionRow.of(textInput))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 2 -> {
                String textId = "message";
                TextInput textInput = TextInput.create(textId, getString("manage_message_label"), TextInputStyle.PARAGRAPH)
                        .setMinLength(1)
                        .setMaxLength(Message.MAX_CONTENT_LENGTH)
                        .setValue(message)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("manage_message_title"), e -> {
                            this.message = e.getValue(textId).getAsString();
                            setLog(LogStatus.SUCCESS, getString("set_message"));
                            return null;
                        }).addActionRows(ActionRow.of(textInput))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 3 -> {
                StandardGuildMessageChannel channel = null;
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

                if (intervalMinutes != null && !ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, getString("err_nopremium"));
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
                Optional.of(event.getGuild())
                        .map(guild -> guild.getTextChannelById(reminderEntity.getConfirmationMessageChannelId()))
                        .ifPresent(ch -> ch.editMessageEmbedsById(reminderEntity.getConfirmationMessageMessageId(), eb.build())
                                .submit()
                                .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE))
                        );

                setLog(LogStatus.SUCCESS, getString("set"));
                setStateReminders();
                return true;
            }
            case 4 -> {
                EntityManagerWrapper entityManager = getEntityManager();
                ReminderEntity reminderEntity = entityManager.find(ReminderEntity.class, id);

                if (reminderEntity != null) {
                    entityManager.getTransaction().begin();
                    entityManager.remove(reminderEntity);
                    entityManager.getTransaction().commit();

                    Optional.of(event.getGuild())
                            .map(guild -> guild.getTextChannelById(reminderEntity.getConfirmationMessageChannelId()))
                            .ifPresent(ch -> ch.deleteMessageById(reminderEntity.getConfirmationMessageMessageId())
                                    .submit()
                                    .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE))
                            );
                }

                setLog(LogStatus.SUCCESS, getString("set_remove"));
                setStateReminders();
                return true;
            }
        }

        return true;
    }

    @ControllerButton(state = STATE_SET_CHANNEL)
    public boolean onButtonSetChannel(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(STATE_MANAGE);
            return true;
        }
        return false;
    }

    @ControllerEntitySelectMenu(state = STATE_SET_CHANNEL)
    public boolean onEntitySelectMenuSetChannel(EntitySelectInteractionEvent event) {
        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getMentions().getChannels().get(0);
        if (!BotPermissionUtil.canWrite(channel)) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_send", "#" + StringUtil.escapeMarkdownInField(channel.getName())));
            return true;
        }

        this.guildChannel = new AtomicStandardGuildMessageChannel(channel);
        setLog(LogStatus.SUCCESS, getString("set_channel"));
        setState(STATE_MANAGE);
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
        ArrayList<Button> buttons = new ArrayList<>();
        int iStart = type == ReminderEntity.Type.DM_REMINDER ? 2 : 0;
        for (int i = iStart; i < options.length; i++) {
            Button button = Button.of(i == 4 ? ButtonStyle.DANGER : ButtonStyle.PRIMARY, String.valueOf(i), options[i]);
            buttons.add(button);
        }
        setComponents(buttons);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, null,
                getString(type == ReminderEntity.Type.DM_REMINDER ? "manage_dm_title" : "manage_guild_title")
        );

        if (type == ReminderEntity.Type.GUILD_REMINDER) {
            eb.addField(getString("manage_channel"), guildChannel.getPrefixedNameInField(getLocale()), true);
        }
        eb.addField(getString("manage_triggertime"), TimeFormat.DATE_TIME_SHORT.atInstant(triggerTime).toString(), true);
        if (type == ReminderEntity.Type.GUILD_REMINDER) {
            String intervalText = TextManager.getString(getLocale(), Category.UTILITY, "reminder_norep");
            if (intervalMinutes != null && intervalMinutes > 0) {
                intervalText = TimeUtil.getRemainingTimeString(getLocale(), Duration.ofMinutes(intervalMinutes).toMillis(), false);
            }
            eb.addField(getString("manage_interval") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), intervalText, true);
        }
        eb.addField(getString("manage_message"), StringUtil.shortenString(message, 1024), false);
        return eb;
    }

    @Draw(state = STATE_SET_CHANNEL)
    public EmbedBuilder onDrawSetChannel(Member member) {
        EntitySelectMenu selectMenu = EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(ChannelType.TEXT, ChannelType.NEWS)
                .setDefaultValues(EntitySelectMenu.DefaultValue.channel(guildChannel.getIdLong()))
                .build();
        setComponents(selectMenu);
        return EmbedFactory.getEmbedDefault(this, getString("channel_desc"), getString("channel_title"));
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

}
