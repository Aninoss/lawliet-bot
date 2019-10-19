package Commands.ServerManagement;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import General.*;
import General.AutoChannel.AutoChannelData;
import General.Mention.MentionFinder;
import MySQL.DBServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandProperties(
    trigger = "autochannel",
    botPermissions = Permission.CREATE_CHANNELS_ON_SERVER | Permission.MOVE_MEMBERS_ON_SERVER,
    userPermissions = Permission.CREATE_CHANNELS_ON_SERVER | Permission.MOVE_MEMBERS_ON_SERVER,
    emoji = "\uD83D\uDD0A",
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Sound-icon.png",
    executable = true
)
public class AutoChannelCommand extends Command implements onNavigationListener {
    
    private AutoChannelData autoChannelData;

    public AutoChannelCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            autoChannelData = DBServer.getAutoChannelFromServer(event.getServer().get());
            return Response.TRUE;
        }

        switch (state) {
            case 1:
                ArrayList<ServerVoiceChannel> channelList = MentionFinder.getVoiceChannels(event.getMessage(), inputString).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    autoChannelData.setVoiceChannel(channelList.get(0));
                    setLog(LogStatus.SUCCESS, getString("channelset"));
                    setState(0);
                    DBServer.saveAutoChannel(autoChannelData);
                    return Response.TRUE;
                }

            case 2:
                if (inputString.length() > 0 && inputString.length() < 50) {
                    autoChannelData.setChannelName(inputString);
                    setLog(LogStatus.SUCCESS, getString("channelnameset"));
                    setState(0);
                    DBServer.saveAutoChannel(autoChannelData);
                    return Response.TRUE;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", "50"));
                    return Response.FALSE;
                }
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        autoChannelData.setActive(!autoChannelData.isActive());
                        setLog(LogStatus.SUCCESS, getString("activeset", autoChannelData.isActive()));
                        DBServer.saveAutoChannel(autoChannelData);
                        return true;

                    case 1:
                        setState(1);
                        return true;

                    case 2:
                        setState(2);
                        return true;
                }
                return false;

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
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        switch (state) {
            case 0:
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                        .addField(getString("state0_mactive"), Tools.getOnOffForBoolean(getLocale(), autoChannelData.isActive()), true)
                        .addField(getString("state0_mchannel"), Tools.getStringIfNotNull(autoChannelData.getVoiceChannel(), notSet), true)
                        .addField(getString("state0_mchannelname"), replaceVariables(autoChannelData.getChannelName(),
                                "`%VCNAME`",
                                "`%INDEX`",
                                "`%CREATOR`").replace("``", "` `"), true);

            case 1:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 3;
    }

    public static String replaceVariables(String string, String arg1, String arg2, String arg3) {
        return string.replaceAll("(?i)" + Pattern.quote("%vcname"), Matcher.quoteReplacement(arg1))
                .replaceAll("(?i)" + Pattern.quote("%index"), Matcher.quoteReplacement(arg2))
                .replaceAll("(?i)" + Pattern.quote("%creator"), Matcher.quoteReplacement(arg3));
    }
}
