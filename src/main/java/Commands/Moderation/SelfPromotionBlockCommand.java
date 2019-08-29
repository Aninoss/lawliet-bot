package Commands.Moderation;

import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.LogStatus;
import Constants.Permission;
import Constants.Response;
import Constants.SPAction;
import General.*;
import General.Mention.MentionFinder;
import General.SPBlock.SPBlock;
import MySQL.DBServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;

public class SelfPromotionBlockCommand extends Command implements onNavigationListener {
    private SPBlock spBlock;

    public SelfPromotionBlockCommand() {
        super();
        trigger = "spblock";
        privateUse = false;
        botPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.KICK_USER | Permission.BAN_USER;
        userPermissions = Permission.MANAGE_MASSAGES_IN_TEXT_CHANNEL | Permission.KICK_USER | Permission.BAN_USER;
        nsfw = false;
        withLoadingBar = false;
        emoji = "\uD83D\uDEE1️";
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/ok-shield-icon.png";
        executable = true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, boolean firstTime) throws Throwable {
        if (firstTime) {
            spBlock = DBServer.getSPBlockFromServer(event.getServer().get());
            return Response.TRUE;
        }

        switch (state) {
            case 1:
                ArrayList<User> userIgnoredList = MentionFinder.getUsers(event.getMessage(), inputString).getList();
                if (userIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    spBlock.setIgnoredUser(userIgnoredList);
                    DBServer.saveSPBlock(spBlock);
                    setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                    state = 0;
                    return Response.TRUE;
                }

            case 2:
                ArrayList<ServerTextChannel> channelIgnoredList = MentionFinder.getTextChannels(event.getMessage(), inputString).getList();
                if (channelIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    spBlock.setIgnoredChannels(channelIgnoredList);
                    DBServer.saveSPBlock(spBlock);
                    setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                    state = 0;
                    return Response.TRUE;
                }

            case 3:
                ArrayList<User> logRecieverList = MentionFinder.getUsers(event.getMessage(), inputString).getList();
                if (logRecieverList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    spBlock.setLogRecievers(logRecieverList);
                    DBServer.saveSPBlock(spBlock);
                    setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                    state = 0;
                    return Response.TRUE;
                }
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        spBlock.setActive(!spBlock.isActive());
                        DBServer.saveSPBlock(spBlock);
                        setLog(LogStatus.SUCCESS, getString("onoffset", !spBlock.isActive()));
                        return true;

                    case 1:
                        state = 1;
                        return true;

                    case 2:
                        state = 2;
                        return true;

                    case 3:
                        state = 3;
                        return true;

                    case 4:
                        state = 4;
                        return true;

                    /*case 5:
                        spBlock.setBlockName(!spBlock.isBlockName());
                        DBServer.saveSPBlock(spBlock);
                        setLog(LogStatus.SUCCESS, getString("nameblockset", !spBlock.isBlockName()));
                        return true;*/
                }
                return false;

            case 1:
                switch (i) {
                    case -1:
                        state = 0;
                        return true;

                    case 0:
                        spBlock.resetIgnoredUser();
                        DBServer.saveSPBlock(spBlock);
                        state = 0;
                        setLog(LogStatus.SUCCESS, getString("ignoredusersset"));
                        return true;
                }
                return false;

            case 2:
                switch (i) {
                    case -1:
                        state = 0;
                        return true;

                    case 0:
                        spBlock.resetIgnoredChannels();
                        DBServer.saveSPBlock(spBlock);
                        state = 0;
                        setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                        return true;
                }
                return false;

            case 3:
                switch (i) {
                    case -1:
                        state = 0;
                        return true;

                    case 0:
                        spBlock.resetLogRecievers();
                        DBServer.saveSPBlock(spBlock);
                        state = 0;
                        setLog(LogStatus.SUCCESS, getString("logrecieverset"));
                        return true;
                }
                return false;

            case 4:
                if (i == -1) {
                    state = 0;
                    return true;
                } else if (i <= 2) {
                    spBlock.setAction(SPAction.values()[i]);
                    DBServer.saveSPBlock(spBlock);
                    state = 0;
                    setLog(LogStatus.SUCCESS, getString("actionset"));
                    return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api) throws Throwable {
        switch (state) {
            case 0:
                options = getString("state0_options").split("\n");
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description"))
                       .addField(getString("state0_menabled"), Tools.getOnOffForBoolean(locale, spBlock.isActive()), true)
                       .addField(getString("state0_mignoredusers"),ListGen.getUserList(locale, spBlock.getIgnoredUser()), true)
                       .addField(getString("state0_mignoredchannels"),ListGen.getChannelList(locale, spBlock.getIgnoredChannels()), true)
                       .addField(getString("state0_mlogreciever"),ListGen.getUserList(locale, spBlock.getLogRecievers()), true)
                       .addField(getString("state0_maction"),getString("state0_mactionlist").split("\n")[spBlock.getAction().ordinal()], true);

            case 1:
                options = new String[]{getString("empty")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                options = new String[]{getString("empty")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));

            case 3:
                options = new String[]{getString("empty")};
                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title"));

            case 4:
                options = getString("state0_mactionlist").split("\n");
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"), getString("state4_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {
    }

    @Override
    public int getMaxReactionNumber() {
        return 5;
    }
}
