package commands.runnables.moderationcategory;

import commands.Category;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnStaticButtonListener;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.cache.ServerPatreonBoostCache;
import core.utils.*;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.AutoModEntity;
import mysql.hibernate.entity.guild.BanAppealEntity;
import mysql.hibernate.entity.guild.ModerationEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "mod",
        botGuildPermissions = { Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS, Permission.MODERATE_MEMBERS },
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "moderation", "modsettings" }
)
public class ModSettingsCommand extends NavigationAbstract implements OnStaticButtonListener {

    public static int MAX_JAIL_ROLES = 20;
    public static final String BUTTON_ID_UNBAN = "ban_appeals_unban";
    public static final String BUTTON_ID_DECLINE = "ban_appeals_decline";
    public static final String BUTTON_ID_DECLINE_PERMANENTLY = "ban_appeals_decline_permanently";


    private Integer autoKickTemp;
    private Integer autoBanTemp;
    private Integer autoMuteTemp;
    private Integer autoJailTemp;
    private Integer autoBanDaysTemp;
    private Integer autoMuteDaysTemp;
    private Integer autoJailDaysTemp;
    private NavigationHelper<AtomicRole> jailRolesNavigationHelper;

    public ModSettingsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        jailRolesNavigationHelper = new NavigationHelper<>(this, guildEntity -> guildEntity.getModeration().getJailRoles(), AtomicRole.class, MAX_JAIL_ROLES);
        checkRolesWithLog(event.getMember(), getGuildEntity().getModeration().getJailRoles().stream().map(r -> r.get().orElse(null)).collect(Collectors.toList()));
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        ModerationEntity moderation = getGuildEntity().getModeration();

        switch (state) {
            case 1:
                List<TextChannel> channelsList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
                if (channelsList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else {
                    TextChannel channel = channelsList.get(0);
                    if (checkWriteEmbedInChannelWithLog(channel)) {
                        moderation.beginTransaction();
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_NOTIFICATION_CHANNEL, event.getMember(), moderation.getLogChannelId(), channel.getIdLong());
                        moderation.setLogChannelId(channel.getIdLong());
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        setState(0);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        return MessageInputResponse.FAILED;
                    }
                }

            case 2:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoKickTemp = value;
                        setState(4);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return MessageInputResponse.FAILED;
                }

            case 3:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoBanTemp = value;
                        setState(5);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return MessageInputResponse.FAILED;
                }

            case 4:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        moderation.beginTransaction();
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_KICK_WARNS, event.getMember(), moderation.getAutoKick().getInfractions(), autoKickTemp);
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_KICK_WARN_DAYS, event.getMember(), moderation.getAutoKick().getInfractionDays(), value);

                        moderation.getAutoKick().setInfractions(autoKickTemp);
                        moderation.getAutoKick().setInfractionDays(value);
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return MessageInputResponse.FAILED;
                }

            case 5:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoBanDaysTemp = value;
                        setState(7);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return MessageInputResponse.FAILED;
                }

            case 7:
                long minutes = MentionUtil.getTimeMinutes(input).getValue();
                if (minutes > 0) {
                    moderation.beginTransaction();
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_WARNS, event.getMember(), moderation.getAutoBan().getInfractions(), autoBanTemp);
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_WARN_DAYS, event.getMember(), moderation.getAutoBan().getInfractionDays(), autoBanDaysTemp);
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_DURATION, event.getMember(), moderation.getAutoBan().getDurationMinutes(), (int) minutes);

                    moderation.getAutoBan().setInfractions(autoBanTemp);
                    moderation.getAutoBan().setInfractionDays(autoBanDaysTemp);
                    moderation.getAutoBan().setDurationMinutes((int) minutes);
                    moderation.commitTransaction();

                    setLog(LogStatus.SUCCESS, getString("autobanset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
                    return MessageInputResponse.FAILED;
                }

            case 8:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoMuteTemp = value;
                        setState(9);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return MessageInputResponse.FAILED;
                }

            case 9:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoMuteDaysTemp = value;
                        setState(10);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return MessageInputResponse.FAILED;
                }

            case 10:
                minutes = MentionUtil.getTimeMinutes(input).getValue();
                if (minutes > 0) {
                    moderation.beginTransaction();
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_WARNS, event.getMember(), moderation.getAutoMute().getInfractions(), autoMuteTemp);
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_WARN_DAYS, event.getMember(), moderation.getAutoMute().getInfractionDays(), autoMuteDaysTemp);
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_DURATION, event.getMember(), moderation.getAutoMute().getDurationMinutes(), (int) minutes);

                    moderation.getAutoMute().setInfractions(autoMuteTemp);
                    moderation.getAutoMute().setInfractionDays(autoMuteDaysTemp);
                    moderation.getAutoMute().setDurationMinutes((int) minutes);
                    moderation.commitTransaction();

                    setLog(LogStatus.SUCCESS, getString("automuteset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
                    return MessageInputResponse.FAILED;
                }

            case 11:
                List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
                return jailRolesNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMessage().getMember(), 0, BotLogEntity.Event.MOD_JAIL_ROLES);

            case 13:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoJailTemp = value;
                        setState(14);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return MessageInputResponse.FAILED;
                }

            case 14:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoJailDaysTemp = value;
                        setState(15);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return MessageInputResponse.FAILED;
                }

            case 15:
                minutes = MentionUtil.getTimeMinutes(input).getValue();
                if (minutes > 0) {
                    moderation.beginTransaction();
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_WARNS, event.getMember(), moderation.getAutoJail().getInfractions(), autoJailTemp);
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_WARN_DAYS, event.getMember(), moderation.getAutoJail().getInfractionDays(), autoJailDaysTemp);
                    logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_DURATION, event.getMember(), moderation.getAutoJail().getDurationMinutes(), (int) minutes);

                    moderation.getAutoJail().setInfractions(autoJailTemp);
                    moderation.getAutoJail().setInfractionDays(autoJailDaysTemp);
                    moderation.getAutoJail().setDurationMinutes((int) minutes);
                    moderation.commitTransaction();

                    setLog(LogStatus.SUCCESS, getString("autojailset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
                    return MessageInputResponse.FAILED;
                }

            case 16:
                channelsList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
                if (channelsList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else {
                    TextChannel channel = channelsList.get(0);
                    if (checkWriteEmbedInChannelWithLog(channel)) {
                        moderation.beginTransaction();
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_BAN_APPEAL_LOG_CHANNEL, event.getMember(), moderation.getBanAppealLogChannelIdEffectively(), channel.getIdLong());
                        moderation.setBanAppealLogChannelId(channel.getIdLong());
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("banappealchannelset"));
                        setState(0);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        return MessageInputResponse.FAILED;
                    }
                }

            default:
                return null;
        }
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        ModerationEntity moderation = getGuildEntity().getModeration();

        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
                        return false;

                    case 0:
                        setState(1);
                        return true;

                    case 1:
                        moderation.beginTransaction();
                        moderation.setConfirmationMessages(!moderation.getConfirmationMessages());
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_CONFIRMATION_MESSAGES, event.getMember(), null, moderation.getConfirmationMessages());
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("setquestion", moderation.getConfirmationMessages()));
                        return true;

                    case 2:
                        jailRolesNavigationHelper.startDataAdd(11);
                        return true;

                    case 3:
                        jailRolesNavigationHelper.startDataRemove(12);
                        return true;

                    case 4:
                        if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                            return true;
                        }
                        setState(16);
                        return true;

                    case 5:
                        setState(8);
                        return true;

                    case 6:
                        setState(13);
                        return true;

                    case 7:
                        setState(2);
                        return true;

                    case 8:
                        setState(3);
                        return true;

                    default:
                        return false;
                }

            case 1:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_NOTIFICATION_CHANNEL, event.getMember(), moderation.getLogChannelId(), null);
                        moderation.setLogChannelId(null);
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("channelreset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 2:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        moderation.getAutoKick().setInfractions(null);
                        moderation.getAutoKick().setInfractionDays(null);
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_AUTO_KICK_DISABLE, event.getMember());
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 3:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        moderation.getAutoBan().setInfractions(null);
                        moderation.getAutoBan().setInfractionDays(null);
                        moderation.getAutoBan().setDurationMinutes(null);
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_AUTO_BAN_DISABLE, event.getMember());
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("autobanset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 4:
                switch (i) {
                    case -1:
                        setState(2);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_KICK_WARNS, event.getMember(), moderation.getAutoKick().getInfractions(), autoKickTemp);
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_KICK_WARN_DAYS, event.getMember(), moderation.getAutoKick().getInfractionDays(), null);

                        moderation.getAutoKick().setInfractions(autoKickTemp);
                        moderation.getAutoKick().setInfractionDays(null);
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 5:
                switch (i) {
                    case -1:
                        setState(3);
                        return true;

                    case 0:
                        autoBanDaysTemp = null;
                        setState(7);
                        return true;

                    default:
                        return false;
                }

            case 7:
                switch (i) {
                    case -1:
                        setState(5);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_WARNS, event.getMember(), moderation.getAutoBan().getInfractions(), autoBanTemp);
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_WARN_DAYS, event.getMember(), moderation.getAutoBan().getInfractionDays(), autoBanDaysTemp);
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_BAN_DURATION, event.getMember(), moderation.getAutoBan().getDurationMinutes(), null);

                        moderation.getAutoBan().setInfractions(autoBanTemp);
                        moderation.getAutoBan().setInfractionDays(autoBanDaysTemp);
                        moderation.getAutoBan().setDurationMinutes(null);
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("autobanset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 8:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        moderation.getAutoMute().setInfractions(null);
                        moderation.getAutoMute().setInfractionDays(null);
                        moderation.getAutoMute().setDurationMinutes(null);
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_AUTO_MUTE_DISABLE, event.getMember());
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("automuteset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 9:
                switch (i) {
                    case -1:
                        setState(8);
                        return true;

                    case 0:
                        autoMuteDaysTemp = null;
                        setState(10);
                        return true;

                    default:
                        return false;
                }

            case 10:
                switch (i) {
                    case -1:
                        setState(9);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_WARNS, event.getMember(), moderation.getAutoMute().getInfractions(), autoMuteTemp);
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_WARN_DAYS, event.getMember(), moderation.getAutoMute().getInfractionDays(), autoMuteDaysTemp);
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_MUTE_DURATION, event.getMember(), moderation.getAutoMute().getDurationMinutes(), null);

                        moderation.getAutoMute().setInfractions(autoMuteTemp);
                        moderation.getAutoMute().setInfractionDays(autoMuteDaysTemp);
                        moderation.getAutoMute().setDurationMinutes(null);
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("automuteset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 11:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                return false;

            case 12:
                return jailRolesNavigationHelper.removeData(i, event.getMember(), 0, BotLogEntity.Event.MOD_JAIL_ROLES);

            case 13:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        moderation.getAutoJail().setInfractions(null);
                        moderation.getAutoJail().setInfractionDays(null);
                        moderation.getAutoJail().setDurationMinutes(null);
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_AUTO_JAIL_DISABLE, event.getMember());
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("autojailset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 14:
                switch (i) {
                    case -1:
                        setState(13);
                        return true;

                    case 0:
                        autoJailDaysTemp = null;
                        setState(15);
                        return true;

                    default:
                        return false;
                }

            case 15:
                switch (i) {
                    case -1:
                        setState(14);
                        return true;

                    case 0:
                        moderation.beginTransaction();
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_WARNS, event.getMember(), moderation.getAutoJail().getInfractions(), autoJailTemp);
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_WARN_DAYS, event.getMember(), moderation.getAutoJail().getInfractionDays(), autoJailDaysTemp);
                        logAutoMod(BotLogEntity.Event.MOD_AUTO_JAIL_DURATION, event.getMember(), moderation.getAutoJail().getDurationMinutes(), null);

                        moderation.getAutoJail().setInfractions(autoJailTemp);
                        moderation.getAutoJail().setInfractionDays(autoJailDaysTemp);
                        moderation.getAutoJail().setDurationMinutes(null);
                        moderation.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("autojailset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            case 16:
                if (i == -1) {
                    setState(0);
                    return true;
                }

                moderation.beginTransaction();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.MOD_BAN_APPEAL_LOG_CHANNEL, event.getMember(), moderation.getBanAppealLogChannelIdEffectively(), null);
                moderation.setBanAppealLogChannelId(null);
                moderation.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("banappealchannelset"));
                setState(0);
                return true;

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        ModerationEntity moderation = getGuildEntity().getModeration();

        switch (state) {
            case 0:
                Locale locale = getLocale();
                String notSet = TextManager.getString(locale, TextManager.GENERAL, "notset");
                TextChannel textChannel = getTextChannel().get();
                setComponents(getString("state0_options").split("\n"));

                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mchannel"), moderation.getLogChannelId() != null ? moderation.getLogChannel().getPrefixedNameInField(locale) : notSet, true)
                        .addField(getString("state0_mquestion"), StringUtil.getOnOffForBoolean(textChannel, locale, moderation.getConfirmationMessages()), true)
                        .addField(getString("state0_mjailroles"), new ListGen<AtomicRole>().getList(moderation.getJailRoles(), locale, m -> m.getPrefixedNameInField(locale)), true)
                        .addField(getString("state0_mbanappeallogchannel") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), moderation.getBanAppealLogChannelIdEffectively() != null ? moderation.getBanAppealLogChannelEffectively().getPrefixedNameInField(locale) : notSet, true)
                        .addField(getString("state0_mautomod"), getString(
                                "state0_mautomod_desc",
                                getAutoModString(textChannel, moderation.getAutoMute()),
                                getAutoModString(textChannel, moderation.getAutoJail()),
                                getAutoModString(textChannel, moderation.getAutoKick()),
                                getAutoModString(textChannel, moderation.getAutoBan())
                        ), false);

            case 1:
                setComponents(getString("state1_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setComponents(getString("state2_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setComponents(getString("state3_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setComponents(getString("state4_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoKickTemp != 1, StringUtil.numToString(autoKickTemp)), getString("state4_title"));

            case 5:
                setComponents(getString("state4_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoBanTemp != 1, StringUtil.numToString(autoBanTemp)), getString("state5_title"));

            case 7:
                setComponents(getString("state7_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));

            case 8:
                setComponents(getString("state8_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state8_description"), getString("state8_title"));

            case 9:
                setComponents(getString("state4_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoMuteTemp != 1, StringUtil.numToString(autoMuteTemp)), getString("state9_title"));

            case 10:
                setComponents(getString("state10_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state10_description"), getString("state10_title"));

            case 11:
                return jailRolesNavigationHelper.drawDataAdd(getString("state11_title"));

            case 12:
                return jailRolesNavigationHelper.drawDataRemove(getString("state12_title"), getLocale());

            case 13:
                setComponents(getString("state13_options"));
                setLog(LogStatus.WARNING, getString("state13_warning"));
                return EmbedFactory.getEmbedDefault(this, getString("state13_description"), getString("state13_title"));

            case 14:
                setComponents(getString("state4_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoJailTemp != 1, StringUtil.numToString(autoJailTemp)), getString("state14_title"));

            case 15:
                setComponents(getString("state15_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state15_description"), getString("state15_title"));

            case 16:
                setComponents(getString("state16_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state16_description", ExternalLinks.BAN_APPEAL_URL + member.getGuild().getId()), getString("state16_title"));

            default:
                return null;
        }
    }

    private String getAutoModString(TextChannel textChannel, AutoModEntity autoModEntity) {
        if (autoModEntity.getInfractions() == null) {
            return StringUtil.getOnOffForBoolean(textChannel, getLocale(), false);
        }
        return getAutoModString(
                getLocale(),
                autoModEntity.getInfractions(),
                autoModEntity.getInfractionDays(),
                autoModEntity.getDurationMinutes()
        );
    }

    public static String getAutoModString(Locale locale, Integer infractions, Integer infractionDays, Integer durationMinutes) {
        String content = TextManager.getString(locale, Category.MODERATION,"mod_state0_mautomod_templ", infractions != null,
                infractions != null ? StringUtil.numToString(infractions) : "", infractionDays != null
                        ? TextManager.getString(locale, Category.MODERATION, "mod_days", infractionDays > 1, StringUtil.numToString(infractionDays))
                        : TextManager.getString(locale, Category.MODERATION, "mod_total")
        );
        if (durationMinutes != null) {
            content = content + " " + TextManager.getString(locale, Category.MODERATION,"mod_duration", TimeUtil.getRemainingTimeString(locale, durationMinutes * 60_000L, true));
        }
        return content;
    }

    @Override
    public void onStaticButton(@NotNull ButtonInteractionEvent event, @Nullable String secondaryId) {
        long bannedUserId = Long.parseLong(secondaryId);
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                getLocale(),
                event.getMember(),
                new Permission[] { Permission.BAN_MEMBERS },
                new Permission[] { Permission.BAN_MEMBERS }
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
