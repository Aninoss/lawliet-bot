package DiscordListener;

import Constants.PowerPlantStatus;
import Constants.Settings;
import General.Connector;
import MySQL.DBBot;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.event.server.ServerLeaveEvent;

import java.util.ArrayList;
import java.util.Random;

public class ServerJoinListener {
    public ServerJoinListener() {}

    public void onServerJoin(ServerJoinEvent event) {
        try {
            boolean exists = DBServer.serverIsInDatabase(event.getServer());

            DBServer.insertServer(event.getServer());
            DBUser.insertUsers(event.getServer().getMembers());
            event.getApi().getOwner().get().sendMessage("**+++** "+event.getServer().getName() + " (" + event.getServer().getMembers().size() + ")");

            String text = "Hi! Thanks for inviting me to your lovely server! <3\n\nJust write `L.help` to get an overview of all my commands.\nWith `L.tips` you can get some additional tips and tricks.\nFurthermore, use the command `L.language` to change the bot language.\n\nAnd if you need any help, just join the Lawliet Support Server:\n"+ Settings.SERVER_INVITE_URL;

            if (event.getServer().getSystemChannel().isPresent() && event.getServer().getSystemChannel().get().canYouWrite()) {
                event.getServer().getSystemChannel().get().sendMessage(text);
            } else {
                for(ServerTextChannel channel: event.getServer().getTextChannels()) {
                    if (channel.canYouWrite()) {
                        channel.sendMessage(text);
                        break;
                    }
                }
            }

            Connector.updateActivity(event.getApi());

            //Fishery Test
            if (!exists) {
                boolean activeGroup = new Random().nextBoolean();

                if (!activeGroup) DBServer.savePowerPlantStatusSetting(event.getServer(), PowerPlantStatus.STOPPED);
                DBBot.addFisheryTest(event.getServer(), activeGroup);
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
