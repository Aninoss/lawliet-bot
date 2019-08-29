package DiscordListener;
import Commands.ServerManagement.WelcomeCommand;
import General.Tools;
import General.WelcomeMessageSetting;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import java.util.Locale;

public class ServerMemberLeaveListener {
    public ServerMemberLeaveListener(){}

    public void onLeave(ServerMemberLeaveEvent event) {
        if (event.getUser().isYourself()) return;
        Server server = event.getServer();
        Locale locale = null;
        try {
            locale = DBServer.getServerLocale(server);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        //Verabschiedungen
        try {
            WelcomeMessageSetting welcomeMessageSetting = DBServer.getWelcomeMessageSettingFromServer(locale, server);
            if (welcomeMessageSetting != null && welcomeMessageSetting.isGoodbye()) {
                ServerTextChannel channel = welcomeMessageSetting.getFarewellChannel();
                if (channel.canYouWrite()) {
                    channel.sendMessage(
                            WelcomeCommand.replaceVariables(welcomeMessageSetting.getGoodbyeText(),
                                    server.getName(),
                                    "**" + event.getUser().getDiscriminatedName() + "**",
                                    Tools.numToString(locale, server.getMembers().size()))).get();
                }
            }
            if (!event.getUser().isBot()) DBUser.updateOnServerStatus(server, event.getUser(), false);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}