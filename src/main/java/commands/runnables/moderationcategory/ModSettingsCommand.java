package commands.runnables.moderationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListenerOld;
import constants.LogStatus;
import constants.PermissionDeprecated;
import constants.Response;
import core.EmbedFactory;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "mod",
        botPermissions = PermissionDeprecated.KICK_MEMBERS | PermissionDeprecated.BAN_MEMBERS,
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "️⚙️️",
        executableWithoutArgs = true,
        aliases = {"modsettings"}
)
public class ModSettingsCommand extends Command implements OnNavigationListenerOld {

    private ModerationBean moderationBean;
    private int autoKickTemp, autoBanTemp;

    public ModSettingsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        moderationBean = DBModeration.getInstance().getBean(event.getServer().get().getId());
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            case 1:
                ArrayList<ServerTextChannel> channelsList = MentionUtil.getTextChannels(event.getMessage(), inputString).getList();
                if (channelsList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), inputString));
                    return Response.FALSE;
                } else {
                    ServerTextChannel channel = channelsList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        moderationBean.setAnnouncementChannelId(channel.getId());
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 2:
                if (StringUtil.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        autoKickTemp = value;
                        setState(4);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }

            case 3:
                if (StringUtil.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        autoBanTemp = value;
                        setState(5);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }

            case 4:
                if (StringUtil.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        moderationBean.setAutoKick(autoKickTemp, value);
                        setLog(LogStatus.SUCCESS, getString("autokickset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }

            case 5:
                if (StringUtil.stringIsInt(inputString)) {
                    int value = Integer.parseInt(inputString);
                    if (value >= 1) {
                        moderationBean.setAutoBan(autoBanTemp, value);
                        setLog(LogStatus.SUCCESS, getString("autobanset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"too_small2", "1"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL,"no_digit"));
                    return Response.FALSE;
                }

            default:
                return null;
        }
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        removeNavigationWithMessage();
                        return false;

                    case 0:
                        setState(1);
                        return true;

                    case 1:
                        moderationBean.toggleQuestion();
                        setLog(LogStatus.SUCCESS, getString("setquestion", moderationBean.isQuestion()));
                        return true;

                    case 2:
                        setState(2);
                        return true;

                    case 3:
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
                        moderationBean.setAutoBan(0, 0);
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
                        moderationBean.setAutoBan(autoBanTemp, 0);
                        setLog(LogStatus.SUCCESS, getString("autobanset"));
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
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                        .addField(getString("state0_mchannel"), moderationBean.getAnnouncementChannel().map(Mentionable::getMentionTag).orElse(notSet), true)
                        .addField(getString("state0_mquestion"), StringUtil.getOnOffForBoolean(getLocale(), moderationBean.isQuestion()), true)
                        .addField(getString("state0_mautomod"), getString("state0_mautomod_desc", getAutoModString(moderationBean.getAutoKick(), moderationBean.getAutoKickDays()), getAutoModString(moderationBean.getAutoBan(), moderationBean.getAutoBanDays())), false);

            case 1:
                setOptions(new String[]{getString("state1_options")});
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                setOptions(new String[]{getString("state2_options")});
                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setOptions(new String[]{getString("state3_options")});
                return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setOptions(new String[]{getString("state4_options")});
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoKickTemp != 1, StringUtil.numToString(autoKickTemp)), getString("state4_title"));

            case 5:
                setOptions(new String[]{getString("state4_options")});
                return EmbedFactory.getEmbedDefault(this, getString("state4_description", autoBanTemp != 1, StringUtil.numToString(autoBanTemp)), getString("state5_title"));

            default:
                return null;
        }
    }

    private String getAutoModString(int value, int days) throws IOException {
        if (value <= 0) return StringUtil.getOnOffForBoolean(getLocale(), false);
        return getString("state0_mautomod_templ", value > 1, StringUtil.numToString(value), days > 0 ? getString("days", days > 1, StringUtil.numToString(days)) : getString("total"));
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 4;
    }

}
