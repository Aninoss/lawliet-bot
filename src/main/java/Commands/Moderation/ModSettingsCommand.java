package Commands.Moderation;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import General.*;
import General.Mention.MentionFinder;
import MySQL.DBServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;

@CommandProperties(
        trigger = "mod",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\u2699\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/settings-3-icon.png",
        executable = true,
        aliases = {"modsettings"}
)
public class ModSettingsCommand extends Command implements onNavigationListener  {
    
    private ModerationStatus moderationStatus;

    public ModSettingsCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            moderationStatus = DBServer.getModerationFromServer(event.getServer().get());
            return Response.TRUE;
        }

        switch (state) {
            case 1:
                ArrayList<ServerTextChannel> channelsList = MentionFinder.getTextChannels(event.getMessage(), inputString).getList();
                if (channelsList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    ServerTextChannel channel = channelsList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        moderationStatus.setChannel(channel);
                        DBServer.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
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
                        setState(1);
                        return true;

                    case 1:
                        moderationStatus.switchQuestion();
                        DBServer.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("setquestion", moderationStatus.isQuestion()));
                        return true;
                }
                return false;

            case 1:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        moderationStatus.setChannel(null);
                        DBServer.saveModeration(moderationStatus);
                        setLog(LogStatus.SUCCESS, getString("channelreset"));
                        setState(0);
                        return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        switch (state) {
            case 0:
                String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
                setOptions(getString("state0_options").split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                        .addField(getString("state0_mchannel"), Tools.getStringIfNotNull(moderationStatus.getChannel(), notSet), true)
                        .addField(getString("state0_mquestion"), Tools.getOnOffForBoolean(getLocale(), moderationStatus.isQuestion()), true);

            case 1:
                setOptions(new String[]{getString("state1_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 2;
    }
}
