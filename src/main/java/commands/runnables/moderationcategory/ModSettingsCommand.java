package commands.runnables.moderationcategory;

import java.util.List;
import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.ServerMute;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "mod",
        botGuildPermissions = { Permission.MESSAGE_MANAGE, Permission.KICK_MEMBERS, Permission.BAN_MEMBERS },
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        aliases = { "moderation", "modsettings" }
)
public class ModSettingsCommand extends NavigationAbstract {

    private ModerationData moderationBean;
    private int autoKickTemp;
    private int autoBanTemp;
    private int autoMuteTemp;
    private int autoBanDaysTemp;
    private int autoMuteDaysTemp;

    public ModSettingsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        moderationBean = DBModeration.getInstance().retrieve(event.getGuild().getIdLong());
        registerNavigationListener();
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        switch (state) {
            case 1:
                List<TextChannel> channelsList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
                if (channelsList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    TextChannel channel = channelsList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        moderationBean.setAnnouncementChannelId(channel.getIdLong());
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 2:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoKickTemp = value;
                        setState(4);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return Response.FALSE;
                }

            case 3:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoBanTemp = value;
                        setState(5);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return Response.FALSE;
                }

            case 4:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        moderationBean.setAutoKick(autoKickTemp, value);
                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return Response.FALSE;
                }

            case 5:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoBanDaysTemp = value;
                        setState(7);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return Response.FALSE;
                }

            case 6:
                List<Role> roleList = MentionUtil.getRoles(event.getMessage(), input).getList();
                if (roleList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    Role role = roleList.get(0);
                    if (checkRoleWithLog(event.getMember(), role)) {
                        moderationBean.setMuteRoleId(role.getIdLong());
                        setLog(LogStatus.SUCCESS, getString("muteroleset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 7:
                long minutes = MentionUtil.getTimeMinutes(input).getValue();
                if (minutes > 0) {
                    moderationBean.setAutoBan(autoBanTemp, autoBanDaysTemp, (int) minutes);
                    setLog(LogStatus.SUCCESS, getString("autobanset"));
                    setState(0);
                    return Response.TRUE;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid"));
                    return Response.FALSE;
                }

            case 8:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoMuteTemp = value;
                        setState(9);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return Response.FALSE;
                }

            case 9:
                if (StringUtil.stringIsInt(input)) {
                    int value = Integer.parseInt(input);
                    if (value >= 1) {
                        autoMuteDaysTemp = value;
                        setState(10);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                    return Response.FALSE;
                }

            case 10:
                minutes = MentionUtil.getTimeMinutes(input).getValue();
                if (minutes > 0) {
                    moderationBean.setAutoMute(autoMuteTemp, autoMuteDaysTemp, (int) minutes);
                    setLog(LogStatus.SUCCESS, getString("automuteset"));
                    setState(0);
                    return Response.TRUE;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid"));
                    return Response.FALSE;
                }

            default:
                return null;
        }
    }

    @Override
    public boolean controllerButton(ButtonClickEvent event, int i, int state) {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithButtonMessage();
                        return false;

                    case 0:
                        setState(1);
                        return true;

                    case 1:
                        moderationBean.toggleQuestion();
                        setLog(LogStatus.SUCCESS, getString("setquestion", moderationBean.isQuestion()));
                        return true;

                    case 2:
                        setState(6);
                        return true;

                    case 3:
                        if (moderationBean.getMuteRole().isPresent()) {
                            setState(8);
                        } else {
                            setLog(LogStatus.FAILURE, getString("nomute"));
                        }
                        return true;

                    case 4:
                        setState(2);
                        return true;

                    case 5:
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
                        moderationBean.setAnnouncementChannelId(null);
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
                        moderationBean.setAutoKick(0, 0);
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
                        moderationBean.setAutoBan(0, 0, 0);
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
                        moderationBean.setAutoKick(autoKickTemp, 0);
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

            case 6:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderationBean.setMuteRoleId(null);
                        setLog(LogStatus.SUCCESS, getString("muterolereset"));
                        setState(0);
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
                        moderationBean.setAutoBan(autoBanTemp, autoBanDaysTemp, 0);
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
                        moderationBean.setAutoMute(0, 0, 0);
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
                        moderationBean.setAutoMute(autoMuteTemp, autoMuteDaysTemp, 0);
                        setLog(LogStatus.SUCCESS, getString("automuteset"));
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
    public EmbedBuilder draw(int state) {
        switch (state) {
            case 0:
                String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
                setOptions(getString("state0_options").split("\n"));

                String content = getString("state0_description");
                List<TextChannel> leakedChannels = ServerMute.getLeakedChannels(getGuild().get());
                if (leakedChannels.size() > 0) {
                    content += "\n\n" + getString("state0_noteffective", leakedChannels.size() != 1, StringUtil.numToString(leakedChannels.size()), "https://discordhelp.net/mute-user");
                }

                return EmbedFactory.getEmbedDefault(this, content)
                        .addField(getString("state0_mchannel"), moderationBean.getAnnouncementChannel().map(IMentionable::getAsMention).orElse(notSet), true)
                        .addField(getString("state0_mquestion"), StringUtil.getOnOffForBoolean(getLocale(), moderationBean.isQuestion()), true)
                        .addField(getString("state0_mmuterole"), moderationBean.getMuteRole().map(IMentionable::getAsMention).orElse(notSet), true)
                        .addField(getString("state0_mautomod"), getString("state0_mautomod_desc",
                                getAutoModString(moderationBean.getAutoMute(), moderationBean.getAutoMuteDays(), moderationBean.getAutoMuteDuration()),
                                getAutoModString(moderationBean.getAutoKick(), moderationBean.getAutoKickDays(), 0),
                                getAutoModString(moderationBean.getAutoBan(), moderationBean.getAutoBanDays(), moderationBean.getAutoBanDuration())
                        ), false);

            case 1:
                setOptions(new String[] { getString("state1_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[] { getString("state2_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[] { getString("state3_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setOptions(new String[] { getString("state4_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoKickTemp != 1, StringUtil.numToString(autoKickTemp)), getString("state4_title"));

            case 5:
                setOptions(new String[] { getString("state4_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoBanTemp != 1, StringUtil.numToString(autoBanTemp)), getString("state5_title"));

            case 6:
                setOptions(new String[] { getString("state6_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state6_description"), getString("state6_title"));

            case 7:
                setOptions(new String[] { getString("state7_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));

            case 8:
                setOptions(new String[] { getString("state8_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state8_description"), getString("state8_title"));

            case 9:
                setOptions(new String[] { getString("state4_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoMuteTemp != 1, StringUtil.numToString(autoMuteTemp)), getString("state9_title"));

            case 10:
                setOptions(new String[] { getString("state10_options") });
                return EmbedFactory.getEmbedDefault(this, getString("state10_description"), getString("state10_title"));

            default:
                return null;
        }
    }

    private String getAutoModString(int value, int days, int duration) {
        if (value <= 0) return StringUtil.getOnOffForBoolean(getLocale(), false);
        String content = getString("state0_mautomod_templ", value > 1, StringUtil.numToString(value), days > 0 ? getString("days", days > 1, StringUtil.numToString(days)) : getString("total"));
        if (duration > 0) {
            content = content + " " + getString("duration", TimeUtil.getRemainingTimeString(getLocale(), duration * 60_000L, true));
        }
        return content;
    }

}
