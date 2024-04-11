package commands.runnables.moderationcategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticButtonListener;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.RolesStateProcessor;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.cache.ServerPatreonBoostCache;
import core.modals.DurationModalBuilder;
import core.modals.IntModalBuilder;
import core.utils.*;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.AutoModEntity;
import mysql.hibernate.entity.guild.BanAppealEntity;
import mysql.hibernate.entity.guild.ModerationEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "mod",
        botGuildPermissions = {Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS, Permission.MODERATE_MEMBERS},
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"moderation", "modsettings", "modconfig"}
)
public class ModSettingsCommand extends NavigationAbstract implements OnStaticButtonListener {

    public static int MAX_JAIL_ROLES = 20;
    public static final String BUTTON_ID_UNBAN = "ban_appeals_unban";
    public static final String BUTTON_ID_DECLINE = "ban_appeals_decline";
    public static final String BUTTON_ID_DECLINE_PERMANENTLY = "ban_appeals_decline_permanently";

    private static final int STATE_SET_LOG_CHANNEL = 1,
            STATE_SET_JAIL_ROLES = 11,
            STATE_SET_BAN_APPEAL_LOG_CHANNEL = 16,
            STATE_SET_AUTO_MUTE_WARNS = 8,
            STATE_SET_AUTO_MUTE_WARN_DAYS = 9,
            STATE_SET_AUTO_MUTE_DURATION = 10,
            STATE_SET_AUTO_JAIL_WARNS = 13,
            STATE_SET_AUTO_JAIL_WARN_DAYS = 14,
            STATE_SET_AUTO_JAIL_DURATION = 15,
            STATE_SET_AUTO_KICK_WARNS = 2,
            STATE_SET_AUTO_KICK_WARN_DAYS = 4,
            STATE_SET_AUTO_BAN_WARNS = 3,
            STATE_SET_AUTO_BAN_WARN_DAYS = 5,
            STATE_SET_AUTO_BAN_DURATION = 7;

    private Integer autoKickTemp;
    private Integer autoBanTemp;
    private Integer autoMuteTemp;
    private Integer autoJailTemp;
    private Integer autoBanDaysTemp;
    private Integer autoMuteDaysTemp;
    private Integer autoJailDaysTemp;

    public ModSettingsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        checkRolesWithLog(event.getMember(), getGuildEntity().getModeration().getJailRoles().stream().map(r -> r.get().orElse(null)).collect(Collectors.toList()));
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_LOG_CHANNEL, DEFAULT_STATE, getString("state0_mchannel"))
                        .setMinMax(0, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
                        .setLogEvent(BotLogEntity.Event.MOD_NOTIFICATION_CHANNEL)
                        .setSingleGetter(() -> getGuildEntity().getModeration().getLogChannelId())
                        .setSingleSetter(channelId -> getGuildEntity().getModeration().setLogChannelId(channelId)),
                new RolesStateProcessor(this, STATE_SET_JAIL_ROLES, DEFAULT_STATE, getString("state0_mjailroles"))
                        .setMinMax(0, MAX_JAIL_ROLES)
                        .setCheckAccess(true)
                        .setLogEvent(BotLogEntity.Event.MOD_JAIL_ROLES)
                        .setGetter(() -> getGuildEntity().getModeration().getJailRoleIds())
                        .setSetter(roleIds -> getGuildEntity().getModeration().setJailRoleIds(roleIds)),
                new GuildChannelsStateProcessor(this, STATE_SET_BAN_APPEAL_LOG_CHANNEL, DEFAULT_STATE, getString("state0_mbanappeallogchannel"))
                        .setMinMax(0, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
                        .setDescription(getString("state16_description", ExternalLinks.BAN_APPEAL_URL + event.getGuild().getId()))
                        .setLogEvent(BotLogEntity.Event.MOD_BAN_APPEAL_LOG_CHANNEL)
                        .setSingleGetter(() -> getGuildEntity().getModeration().getBanAppealLogChannelIdEffectively())
                        .setSingleSetter(channelId -> getGuildEntity().getModeration().setBanAppealLogChannelId(channelId))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                setState(STATE_SET_LOG_CHANNEL);
                return true;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                moderation.setConfirmationMessages(!moderation.getConfirmationMessages());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_CONFIRMATION_MESSAGES, event.getMember(), null, moderation.getConfirmationMessages());
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("setquestion", moderation.getConfirmationMessages()));
                return true;

            case 2:
                setState(STATE_SET_JAIL_ROLES);
                return true;

            case 3:
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                    return true;
                }
                setState(STATE_SET_BAN_APPEAL_LOG_CHANNEL);
                return true;

            case 4:
                setState(STATE_SET_AUTO_MUTE_WARNS);
                return true;

            case 5:
                setState(STATE_SET_AUTO_JAIL_WARNS);
                return true;

            case 6:
                setState(STATE_SET_AUTO_KICK_WARNS);
                return true;

            case 7:
                setState(STATE_SET_AUTO_BAN_WARNS);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_MUTE_WARNS)
    public boolean onButtonAutoMuteWarns(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(DEFAULT_STATE);
                return true;

            case 0:
                Modal modal = new IntModalBuilder(this, getString("automod_warns"))
                        .setMinMax(1, 999)
                        .setGetter(() -> getGuildEntity().getModeration().getAutoMute().getInfractions())
                        .setSetterOptionalLogs(value -> {
                            autoMuteTemp = value;
                            setState(STATE_SET_AUTO_MUTE_WARN_DAYS);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                moderation.getAutoMute().setInfractions(null);
                moderation.getAutoMute().setInfractionDays(null);
                moderation.getAutoMute().setDurationMinutes(null);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_AUTO_MUTE_DISABLE, event.getMember());
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("automuteset"));
                setState(DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_MUTE_WARN_DAYS)
    public boolean onButtonAutoMuteWarnDays(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(STATE_SET_AUTO_MUTE_WARNS);
                return true;

            case 0:
                Modal modal = new IntModalBuilder(this, getString("automod_warndays"))
                        .setMinMax(1, 999)
                        .setGetter(() -> getGuildEntity().getModeration().getAutoMute().getInfractionDays())
                        .setSetterOptionalLogs(value -> {
                            autoMuteDaysTemp = value;
                            setState(STATE_SET_AUTO_MUTE_DURATION);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                autoMuteDaysTemp = null;
                setState(STATE_SET_AUTO_MUTE_DURATION);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_MUTE_DURATION)
    public boolean onButtonAutoMuteDuration(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(STATE_SET_AUTO_MUTE_WARN_DAYS);
                return true;

            case 0:
                Modal modal = new DurationModalBuilder(this, getString("automod_duration"))
                        .setMinMaxMinutes(1, Integer.MAX_VALUE)
                        .enableHibernateTransaction()
                        .setGetterInt(() -> getGuildEntity().getModeration().getAutoMute().getDurationMinutes())
                        .setSetterIntOptionalLogs(value -> {
                            ModerationEntity moderation = getGuildEntity().getModeration();
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_WARNS, event.getMember(), moderation.getAutoMute().getInfractions(), autoMuteTemp);
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_WARN_DAYS, event.getMember(), moderation.getAutoMute().getInfractionDays(), autoMuteDaysTemp);
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_DURATION, event.getMember(), moderation.getAutoMute().getDurationMinutes(), value);

                            moderation.getAutoMute().setInfractions(autoMuteTemp);
                            moderation.getAutoMute().setInfractionDays(autoMuteDaysTemp);
                            moderation.getAutoMute().setDurationMinutes(value);

                            setLog(LogStatus.SUCCESS, getString("automuteset"));
                            setState(DEFAULT_STATE);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_WARNS, event.getMember(), moderation.getAutoMute().getInfractions(), autoMuteTemp);
                logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_WARN_DAYS, event.getMember(), moderation.getAutoMute().getInfractionDays(), autoMuteDaysTemp);
                logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_DURATION, event.getMember(), moderation.getAutoMute().getDurationMinutes(), null);

                moderation.getAutoMute().setInfractions(autoMuteTemp);
                moderation.getAutoMute().setInfractionDays(autoMuteDaysTemp);
                moderation.getAutoMute().setDurationMinutes(null);
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("automuteset"));
                setState(DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_JAIL_WARNS)
    public boolean onButtonAutoJailWarns(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(DEFAULT_STATE);
                return true;

            case 0:
                Modal modal = new IntModalBuilder(this, getString("automod_warns"))
                        .setMinMax(1, 999)
                        .setGetter(() -> getGuildEntity().getModeration().getAutoJail().getInfractions())
                        .setSetterOptionalLogs(value -> {
                            autoJailTemp = value;
                            setState(STATE_SET_AUTO_JAIL_WARN_DAYS);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                moderation.getAutoJail().setInfractions(null);
                moderation.getAutoJail().setInfractionDays(null);
                moderation.getAutoJail().setDurationMinutes(null);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_AUTO_JAIL_DISABLE, event.getMember());
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("autojailset"));
                setState(DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_JAIL_WARN_DAYS)
    public boolean onButtonAutoJailWarnDays(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(STATE_SET_AUTO_JAIL_WARNS);
                return true;

            case 0:
                Modal modal = new IntModalBuilder(this, getString("automod_warndays"))
                        .setMinMax(1, 999)
                        .setGetter(() -> getGuildEntity().getModeration().getAutoJail().getInfractionDays())
                        .setSetterOptionalLogs(value -> {
                            autoJailDaysTemp = value;
                            setState(STATE_SET_AUTO_JAIL_DURATION);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                autoJailDaysTemp = null;
                setState(STATE_SET_AUTO_JAIL_DURATION);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_JAIL_DURATION)
    public boolean onButtonAutoJailDuration(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(STATE_SET_AUTO_JAIL_WARN_DAYS);
                return true;

            case 0:
                Modal modal = new DurationModalBuilder(this, getString("automod_duration"))
                        .setMinMaxMinutes(1, Integer.MAX_VALUE)
                        .enableHibernateTransaction()
                        .setGetterInt(() -> getGuildEntity().getModeration().getAutoJail().getDurationMinutes())
                        .setSetterIntOptionalLogs(value -> {
                            ModerationEntity moderation = getGuildEntity().getModeration();
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_WARNS, event.getMember(), moderation.getAutoJail().getInfractions(), autoJailTemp);
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_WARN_DAYS, event.getMember(), moderation.getAutoJail().getInfractionDays(), autoJailDaysTemp);
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_DURATION, event.getMember(), moderation.getAutoJail().getDurationMinutes(), value);

                            moderation.getAutoJail().setInfractions(autoJailTemp);
                            moderation.getAutoJail().setInfractionDays(autoJailDaysTemp);
                            moderation.getAutoJail().setDurationMinutes(value);

                            setLog(LogStatus.SUCCESS, getString("autojailset"));
                            setState(DEFAULT_STATE);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_WARNS, event.getMember(), moderation.getAutoJail().getInfractions(), autoJailTemp);
                logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_WARN_DAYS, event.getMember(), moderation.getAutoJail().getInfractionDays(), autoJailDaysTemp);
                logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_DURATION, event.getMember(), moderation.getAutoJail().getDurationMinutes(), null);

                moderation.getAutoJail().setInfractions(autoJailTemp);
                moderation.getAutoJail().setInfractionDays(autoJailDaysTemp);
                moderation.getAutoJail().setDurationMinutes(null);
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("autojailset"));
                setState(DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_KICK_WARNS)
    public boolean onButtonAutoKickWarns(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(DEFAULT_STATE);
                return true;

            case 0:
                Modal modal = new IntModalBuilder(this, getString("automod_warns"))
                        .setMinMax(1, 999)
                        .setGetter(() -> getGuildEntity().getModeration().getAutoKick().getInfractions())
                        .setSetterOptionalLogs(value -> {
                            autoKickTemp = value;
                            setState(STATE_SET_AUTO_KICK_WARN_DAYS);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                moderation.getAutoKick().setInfractions(null);
                moderation.getAutoKick().setInfractionDays(null);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_AUTO_KICK_DISABLE, event.getMember());
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("autokickset"));
                setState(DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_KICK_WARN_DAYS)
    public boolean onButtonAutoKickWarnDays(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(STATE_SET_AUTO_KICK_WARNS);
                return true;

            case 0:
                Modal modal = new IntModalBuilder(this, getString("automod_warndays"))
                        .setMinMax(1, 999)
                        .enableHibernateTransaction()
                        .setGetter(() -> getGuildEntity().getModeration().getAutoKick().getInfractionDays())
                        .setSetterOptionalLogs(value -> {
                            ModerationEntity moderation = getGuildEntity().getModeration();
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_KICK_WARNS, event.getMember(), moderation.getAutoKick().getInfractions(), autoKickTemp);
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_KICK_WARN_DAYS, event.getMember(), moderation.getAutoKick().getInfractionDays(), value);

                            moderation.getAutoKick().setInfractions(autoKickTemp);
                            moderation.getAutoKick().setInfractionDays(value);

                            setLog(LogStatus.SUCCESS, getString("autokickset"));
                            setState(DEFAULT_STATE);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                logAutoMod(BotLogEntity.Event.MOD_AUTO_KICK_WARNS, event.getMember(), moderation.getAutoKick().getInfractions(), autoKickTemp);
                logAutoMod(BotLogEntity.Event.MOD_AUTO_KICK_WARN_DAYS, event.getMember(), moderation.getAutoKick().getInfractionDays(), null);

                moderation.getAutoKick().setInfractions(autoKickTemp);
                moderation.getAutoKick().setInfractionDays(null);
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("autokickset"));
                setState(DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_BAN_WARNS)
    public boolean onButtonAutoBanWarns(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(DEFAULT_STATE);
                return true;

            case 0:
                Modal modal = new IntModalBuilder(this, getString("automod_warns"))
                        .setMinMax(1, 999)
                        .setGetter(() -> getGuildEntity().getModeration().getAutoBan().getInfractions())
                        .setSetterOptionalLogs(value -> {
                            autoBanTemp = value;
                            setState(STATE_SET_AUTO_BAN_WARN_DAYS);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                moderation.getAutoBan().setInfractions(null);
                moderation.getAutoBan().setInfractionDays(null);
                moderation.getAutoBan().setDurationMinutes(null);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_AUTO_BAN_DISABLE, event.getMember());
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("autobanset"));
                setState(DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_BAN_WARN_DAYS)
    public boolean onButtonAutoBanWarnDays(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(STATE_SET_AUTO_BAN_WARNS);
                return true;

            case 0:
                Modal modal = new IntModalBuilder(this, getString("automod_warndays"))
                        .setMinMax(1, 999)
                        .setGetter(() -> getGuildEntity().getModeration().getAutoBan().getInfractionDays())
                        .setSetterOptionalLogs(value -> {
                            autoBanDaysTemp = value;
                            setState(STATE_SET_AUTO_BAN_DURATION);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                autoBanDaysTemp = null;
                setState(STATE_SET_AUTO_BAN_DURATION);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_SET_AUTO_BAN_DURATION)
    public boolean onButtonAutoBanDuration(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(STATE_SET_AUTO_BAN_WARN_DAYS);
                return true;

            case 0:
                Modal modal = new DurationModalBuilder(this, getString("automod_duration"))
                        .setMinMaxMinutes(1, Integer.MAX_VALUE)
                        .enableHibernateTransaction()
                        .setGetterInt(() -> getGuildEntity().getModeration().getAutoBan().getDurationMinutes())
                        .setSetterIntOptionalLogs(value -> {
                            ModerationEntity moderation = getGuildEntity().getModeration();
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_WARNS, event.getMember(), moderation.getAutoBan().getInfractions(), autoBanTemp);
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_WARN_DAYS, event.getMember(), moderation.getAutoBan().getInfractionDays(), autoBanDaysTemp);
                            logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_DURATION, event.getMember(), moderation.getAutoBan().getDurationMinutes(), value);

                            moderation.getAutoBan().setInfractions(autoBanTemp);
                            moderation.getAutoBan().setInfractionDays(autoBanDaysTemp);
                            moderation.getAutoBan().setDurationMinutes(value);

                            setLog(LogStatus.SUCCESS, getString("autobanset"));
                            setState(DEFAULT_STATE);
                            return false;
                        })
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                ModerationEntity moderation = getGuildEntity().getModeration();
                moderation.beginTransaction();
                logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_WARNS, event.getMember(), moderation.getAutoBan().getInfractions(), autoBanTemp);
                logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_WARN_DAYS, event.getMember(), moderation.getAutoBan().getInfractionDays(), autoBanDaysTemp);
                logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_DURATION, event.getMember(), moderation.getAutoBan().getDurationMinutes(), null);

                moderation.getAutoBan().setInfractions(autoBanTemp);
                moderation.getAutoBan().setInfractionDays(autoBanDaysTemp);
                moderation.getAutoBan().setDurationMinutes(null);
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("autobanset"));
                setState(DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        ModerationEntity moderation = getGuildEntity().getModeration();
        Locale locale = getLocale();
        String notSet = TextManager.getString(locale, TextManager.GENERAL, "notset");
        GuildMessageChannel channel = getGuildMessageChannel().get();
        setComponents(getString("state0_options").split("\n"));

        return EmbedFactory.getEmbedDefault(this)
                .addField(getString("state0_mchannel"), moderation.getLogChannelId() != null ? moderation.getLogChannel().getPrefixedNameInField(locale) : notSet, true)
                .addField(getString("state0_mquestion"), StringUtil.getOnOffForBoolean(channel, locale, moderation.getConfirmationMessages()), true)
                .addField(getString("state0_mjailroles"), new ListGen<AtomicRole>().getList(moderation.getJailRoles(), locale, m -> m.getPrefixedNameInField(locale)), true)
                .addField(getString("state0_mbanappeallogchannel") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), moderation.getBanAppealLogChannelIdEffectively() != null ? moderation.getBanAppealLogChannelEffectively().getPrefixedNameInField(locale) : notSet, true)
                .addField(getString("state0_mautomod"), getString(
                        "state0_mautomod_desc",
                        getAutoModString(channel, moderation.getAutoMute()),
                        getAutoModString(channel, moderation.getAutoJail()),
                        getAutoModString(channel, moderation.getAutoKick()),
                        getAutoModString(channel, moderation.getAutoBan())
                ), false);
    }

    @Draw(state = STATE_SET_AUTO_MUTE_WARNS)
    public EmbedBuilder drawAutoMuteWarns(Member member) {
        setComponents(getString("automod_warns_options").split("\n"), new int[0], new int[]{1});
        return EmbedFactory.getEmbedDefault(this, getString("state8_description"), getString("state8_title"));
    }

    @Draw(state = STATE_SET_AUTO_MUTE_WARN_DAYS)
    public EmbedBuilder drawAutoMuteWarnDays(Member member) {
        setComponents(getString("automod_warndays_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoMuteTemp != 1, StringUtil.numToString(autoMuteTemp)), getString("state9_title"));
    }

    @Draw(state = STATE_SET_AUTO_MUTE_DURATION)
    public EmbedBuilder drawAutoMuteDuration(Member member) {
        setComponents(getString("state10_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state10_description"), getString("state10_title"));
    }

    @Draw(state = STATE_SET_AUTO_JAIL_WARNS)
    public EmbedBuilder drawAutoJailWarns(Member member) {
        setComponents(getString("automod_warns_options").split("\n"), new int[0], new int[]{1});
        setLog(LogStatus.WARNING, getString("state13_warning"));
        return EmbedFactory.getEmbedDefault(this, getString("state13_description"), getString("state13_title"));
    }

    @Draw(state = STATE_SET_AUTO_JAIL_WARN_DAYS)
    public EmbedBuilder drawAutoJailWarnDays(Member member) {
        setComponents(getString("automod_warndays_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoJailTemp != 1, StringUtil.numToString(autoJailTemp)), getString("state14_title"));
    }

    @Draw(state = STATE_SET_AUTO_JAIL_DURATION)
    public EmbedBuilder drawAutoJailDuration(Member member) {
        setComponents(getString("state15_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state15_description"), getString("state15_title"));
    }

    @Draw(state = STATE_SET_AUTO_KICK_WARNS)
    public EmbedBuilder drawAutoKickWarns(Member member) {
        setComponents(getString("automod_warns_options").split("\n"), new int[0], new int[]{1});
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_SET_AUTO_KICK_WARN_DAYS)
    public EmbedBuilder drawAutoKickWarnDays(Member member) {
        setComponents(getString("automod_warndays_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoKickTemp != 1, StringUtil.numToString(autoKickTemp)), getString("state4_title"));
    }

    @Draw(state = STATE_SET_AUTO_BAN_WARNS)
    public EmbedBuilder drawAutoBanWarns(Member member) {
        setComponents(getString("automod_warns_options").split("\n"), new int[0], new int[]{1});
        return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));
    }

    @Draw(state = STATE_SET_AUTO_BAN_WARN_DAYS)
    public EmbedBuilder drawAutoBanWarnDays(Member member) {
        setComponents(getString("automod_warndays_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoBanTemp != 1, StringUtil.numToString(autoBanTemp)), getString("state5_title"));
    }

    @Draw(state = STATE_SET_AUTO_BAN_DURATION)
    public EmbedBuilder drawAutoBanDuration(Member member) {
        setComponents(getString("state7_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state7_description", autoBanTemp != 1, StringUtil.numToString(autoBanTemp)), getString("state7_title"));
    }

    private String getAutoModString(GuildMessageChannel channel, AutoModEntity autoModEntity) {
        if (autoModEntity.getInfractions() == null) {
            return StringUtil.getOnOffForBoolean(channel, getLocale(), false);
        }
        return getAutoModString(
                getLocale(),
                autoModEntity.getInfractions(),
                autoModEntity.getInfractionDays(),
                autoModEntity.getDurationMinutes()
        );
    }

    public static String getAutoModString(Locale locale, Integer infractions, Integer infractionDays, Integer durationMinutes) {
        String content = TextManager.getString(locale, Category.MODERATION, "mod_state0_mautomod_templ", infractions != null,
                infractions != null ? StringUtil.numToString(infractions) : "", infractionDays != null
                        ? TextManager.getString(locale, Category.MODERATION, "mod_days", infractionDays > 1, StringUtil.numToString(infractionDays))
                        : TextManager.getString(locale, Category.MODERATION, "mod_total")
        );
        if (durationMinutes != null) {
            content = content + " " + TextManager.getString(locale, Category.MODERATION, "mod_duration", TimeUtil.getDurationString(Duration.ofMinutes(durationMinutes)));
        }
        return content;
    }

    @Override
    public void onStaticButton(@NotNull ButtonInteractionEvent event, @Nullable String secondaryId) {
        long bannedUserId = Long.parseLong(secondaryId);
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                getLocale(),
                event.getMember(),
                new Permission[]{Permission.BAN_MEMBERS},
                new Permission[]{Permission.BAN_MEMBERS}
        );
        if (errEmbed != null) {
            event.replyEmbeds(errEmbed.build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder(event.getMessage().getEmbeds().get(0));
        ModerationEntity moderationEntity = getGuildEntity().getModeration();
        DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong()).remove(event.getMessageIdLong());

        switch (event.getButton().getId()) {
            case BUTTON_ID_UNBAN -> {
                event.getGuild().unban(UserSnowflake.fromId(bannedUserId))
                        .reason(getString("banappeals"))
                        .queue();

                moderationEntity.beginTransaction();
                moderationEntity.getBanAppeals().remove(bannedUserId);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.BAN_APPEAL_UNBAN, event.getMember(), null, null, List.of(bannedUserId));
                moderationEntity.commitTransaction();

                EmbedUtil.addLog(eb, LogStatus.SUCCESS, getString("banappeals_unban"));
            }
            case BUTTON_ID_DECLINE -> {
                moderationEntity.beginTransaction();
                moderationEntity.getBanAppeals().remove(bannedUserId);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.BAN_APPEAL_DECLINE, event.getMember(), null, null, List.of(bannedUserId));
                moderationEntity.commitTransaction();

                EmbedUtil.addLog(eb, LogStatus.SUCCESS, getString("banappeals_decline"));
            }
            case BUTTON_ID_DECLINE_PERMANENTLY -> {
                BanAppealEntity banAppealEntity = moderationEntity.getBanAppeals().get(bannedUserId);
                if (banAppealEntity == null) {
                    EmbedUtil.addLog(eb, LogStatus.FAILURE, getString("banappeals_decline_permanently_not_found"));
                    break;
                }

                moderationEntity.beginTransaction();
                banAppealEntity.setOpen(false);
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.BAN_APPEAL_DECLINE_PERMANENTLY, event.getMember(), null, null, List.of(bannedUserId));
                moderationEntity.commitTransaction();

                EmbedUtil.addLog(eb, LogStatus.SUCCESS, getString("banappeals_decline_permanently"));
            }
        }

        event.editMessageEmbeds(eb.build())
                .setComponents()
                .queue();
    }

    private void logAutoMod(BotLogEntity.Event event, Member member, Integer value0, Integer value1) {
        BotLogEntity.log(getEntityManager(), event, member, value0, value1);
    }

}
