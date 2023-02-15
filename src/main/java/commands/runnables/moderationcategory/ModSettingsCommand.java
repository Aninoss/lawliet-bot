package commands.runnables.moderationcategory;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.Category;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicTextChannel;
import core.atomicassets.MentionableAtomicAsset;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "mod",
        botGuildPermissions = { Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS, Permission.MODERATE_MEMBERS },
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "moderation", "modsettings" }
)
public class ModSettingsCommand extends NavigationAbstract {

    public static int MAX_JAIL_ROLES = 20;

    private ModerationData moderationData;
    private int autoKickTemp;
    private int autoBanTemp;
    private int autoMuteTemp;
    private int autoJailTemp;
    private int autoBanDaysTemp;
    private int autoMuteDaysTemp;
    private int autoJailDaysTemp;
    private CustomObservableList<AtomicRole> jailRoles;
    private NavigationHelper<AtomicRole> jailRolesNavigationHelper;

    public ModSettingsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        moderationData = DBModeration.getInstance().retrieve(event.getGuild().getIdLong());
        jailRoles = AtomicRole.transformIdList(event.getGuild(), moderationData.getJailRoleIds());
        jailRolesNavigationHelper = new NavigationHelper<>(this, jailRoles, AtomicRole.class, MAX_JAIL_ROLES);
        checkRolesWithLog(event.getMember(), jailRoles.stream().map(r -> r.get().orElse(null)).collect(Collectors.toList()));
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        switch (state) {
            case 1:
                List<TextChannel> channelsList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
                if (channelsList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else {
                    TextChannel channel = channelsList.get(0);
                    if (checkWriteEmbedInChannelWithLog(channel)) {
                        moderationData.setAnnouncementChannelId(channel.getIdLong());
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
                        moderationData.setAutoKick(autoKickTemp, value);
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
                    moderationData.setAutoBan(autoBanTemp, autoBanDaysTemp, (int) minutes);
                    setLog(LogStatus.SUCCESS, getString("autobanset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid"));
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
                    moderationData.setAutoMute(autoMuteTemp, autoMuteDaysTemp, (int) minutes);
                    setLog(LogStatus.SUCCESS, getString("automuteset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid"));
                    return MessageInputResponse.FAILED;
                }

            case 11:
                List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
                return jailRolesNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMessage().getMember(), 0);

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
                    moderationData.setAutoJail(autoJailTemp, autoJailDaysTemp, (int) minutes);
                    setLog(LogStatus.SUCCESS, getString("autojailset"));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid"));
                    return MessageInputResponse.FAILED;
                }

            default:
                return null;
        }
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
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
                        moderationData.toggleQuestion();
                        setLog(LogStatus.SUCCESS, getString("setquestion", moderationData.getQuestion()));
                        return true;

                    case 2:
                        jailRolesNavigationHelper.startDataAdd(11);
                        return true;

                    case 3:
                        jailRolesNavigationHelper.startDataRemove(12);
                        return true;

                    case 4:
                        setState(8);
                        return true;

                    case 5:
                        setState(13);
                        return true;

                    case 6:
                        setState(2);
                        return true;

                    case 7:
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
                        moderationData.setAnnouncementChannelId(null);
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
                        moderationData.setAutoKick(0, 0);
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
                        moderationData.setAutoBan(0, 0, 0);
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
                        moderationData.setAutoKick(autoKickTemp, 0);
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
                        autoBanDaysTemp = 0;
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
                        moderationData.setAutoBan(autoBanTemp, autoBanDaysTemp, 0);
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
                        moderationData.setAutoMute(0, 0, 0);
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
                        autoMuteDaysTemp = 0;
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
                        moderationData.setAutoMute(autoMuteTemp, autoMuteDaysTemp, 0);
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
                return jailRolesNavigationHelper.removeData(i, 0);

            case 13:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderationData.setAutoJail(0, 0, 0);
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
                        autoJailDaysTemp = 0;
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
                        moderationData.setAutoJail(autoJailTemp, autoJailDaysTemp, 0);
                        setLog(LogStatus.SUCCESS, getString("autojailset"));
                        setState(0);
                        return true;

                    default:
                        return false;
                }

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        switch (state) {
            case 0:
                String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
                TextChannel textChannel = getTextChannel().get();
                setComponents(getString("state0_options").split("\n"));

                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mchannel"), moderationData.getAnnouncementChannel().map(c -> new AtomicTextChannel(c).getPrefixedNameInField()).orElse(notSet), true)
                        .addField(getString("state0_mquestion"), StringUtil.getOnOffForBoolean(textChannel, getLocale(), moderationData.getQuestion()), true)
                        .addField(getString("state0_mjailroles"), new ListGen<AtomicRole>().getList(jailRoles, getLocale(), MentionableAtomicAsset::getPrefixedNameInField), true)
                        .addField(getString("state0_mautomod"), getString(
                                "state0_mautomod_desc",
                                getAutoModString(textChannel, moderationData.getAutoMute(), moderationData.getAutoMuteDays(), moderationData.getAutoMuteDuration()),
                                getAutoModString(textChannel, moderationData.getAutoJail(), moderationData.getAutoJailDays(), moderationData.getAutoJailDuration()),
                                getAutoModString(textChannel, moderationData.getAutoKick(), moderationData.getAutoKickDays(), 0),
                                getAutoModString(textChannel, moderationData.getAutoBan(), moderationData.getAutoBanDays(), moderationData.getAutoBanDuration())
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
                return jailRolesNavigationHelper.drawDataRemove(getString("state12_title"));

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

            default:
                return null;
        }
    }

    private String getAutoModString(TextChannel textChannel, int value, int days, int duration) {
        if (value <= 0) {
            return StringUtil.getOnOffForBoolean(textChannel, getLocale(), false);
        }
        return getAutoModString(getLocale(), value, days, duration);
    }

    public static String getAutoModString(Locale locale, int value, int days, int duration) {
        String content = TextManager.getString(locale, Category.MODERATION,"mod_state0_mautomod_templ", value > 1,
                StringUtil.numToString(value), days > 0
                        ? TextManager.getString(locale, Category.MODERATION, "mod_days", days > 1, StringUtil.numToString(days))
                        : TextManager.getString(locale, Category.MODERATION, "mod_total")
        );
        if (duration > 0) {
            content = content + " " + TextManager.getString(locale, Category.MODERATION,"mod_duration", TimeUtil.getRemainingTimeString(locale, duration * 60_000L, true));
        }
        return content;
    }

}
