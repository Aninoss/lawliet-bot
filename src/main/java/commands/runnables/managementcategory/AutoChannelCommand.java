package commands.runnables.managementcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import commands.Command;
import constants.LogStatus;
import constants.Permission;
import constants.Response;
import core.*;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.AutoChannel;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Nameable;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Locale;

@CommandProperties(
        trigger = "autochannel",
        botPermissions = Permission.MANAGE_CHANNELS_ON_SERVER | Permission.MOVE_MEMBERS,
        userPermissions = Permission.MANAGE_CHANNELS_ON_SERVER | Permission.MOVE_MEMBERS,
        emoji = "🔊",
        executableWithoutArgs = true,
        aliases = { "tempchannel" }
)
public class AutoChannelCommand extends Command implements OnNavigationListener {
    
    private AutoChannelBean autoChannelBean;

    public AutoChannelCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        autoChannelBean = DBAutoChannel.getInstance().getBean(event.getServer().get().getId());
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            case 1:
                ArrayList<ServerVoiceChannel> channelList = MentionUtil.getVoiceChannels(event.getMessage(), inputString).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    autoChannelBean.setParentChannelId(channelList.get(0).getId());
                    setLog(LogStatus.SUCCESS, getString("channelset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 2:
                if (inputString.length() > 0 && inputString.length() < 50) {
                    autoChannelBean.setNameMask(inputString);
                    setLog(LogStatus.SUCCESS, getString("channelnameset"));
                    setState(0);
                    return Response.TRUE;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", "50"));
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
                        autoChannelBean.toggleActive();
                        setLog(LogStatus.SUCCESS, getString("activeset", autoChannelBean.isActive()));
                        return true;

                    case 1:
                        setState(1);
                        return true;

                    case 2:
                        setState(2);
                        return true;

                    case 3:
                        autoChannelBean.toggleLocked();
                        setLog(LogStatus.SUCCESS, getString("lockedset", autoChannelBean.isLocked()));
                        return true;

                    default:
                        return false;
                }

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            default:
                return false;
        }
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                        .addField(getString("state0_mactive"), StringUtil.getOnOffForBoolean(getLocale(), autoChannelBean.isActive()), true)
                        .addField(getString("state0_mchannel"), StringUtil.escapeMarkdown(autoChannelBean.getParentChannel().map(Nameable::getName).orElse(notSet)), true)
                        .addField(getString("state0_mchannelname"), AutoChannel.resolveVariables(StringUtil.escapeMarkdown(autoChannelBean.getNameMask()),
                                "`%VCNAME`",
                                "`%INDEX`",
                                "`%CREATOR`"), true)
                        .addField(getString("state0_mlocked"), getString("state0_mlocked_desc", StringUtil.getOnOffForBoolean(getLocale(), autoChannelBean.isLocked())), true);

            case 1:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            default:
                return null;
        }
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 4;
    }

}
