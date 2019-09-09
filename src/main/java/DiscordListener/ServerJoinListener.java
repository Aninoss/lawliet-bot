package DiscordListener;

import Constants.PowerPlantStatus;
import Constants.Settings;
import General.Connector;
import MySQL.DBBot;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.event.server.ServerLeaveEvent;

import java.util.ArrayList;
import java.util.Random;

public class ServerJoinListener {
    public ServerJoinListener() {}

    public void onServerJoin(ServerJoinEvent event) {
        try {
            DBServer.insertServer(event.getServer());
            DBUser.insertUsers(event.getServer().getMembers());
            sendNewMessage(event.getServer());
            event.getApi().getOwner().get().sendMessage("**+++** "+event.getServer().getName() + " (" + event.getServer().getMembers().size() + ")");

            Connector.updateActivity(event.getApi());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void sendNewMessage(Server server) {
        String text = "Hi! Thanks for inviting me to your lovely server! <3\n\nJust write `L.help` to get an overview of all my commands.\nWith `L.fishery` you can configure the tactical \"level system\" of the bot and read how it works.\nFurthermore, use the command `L.language` to change the bot language.\n\nAnd if you need any help, just join the Lawliet Support Server:\n"+ Settings.SERVER_INVITE_URL;

        if (server.getSystemChannel().isPresent() && server.getSystemChannel().get().canYouWrite()) {
            server.getSystemChannel().get().sendMessage(text);
        } else {
            for(ServerTextChannel channel: server.getTextChannels()) {
                if (channel.canYouWrite()) {
                    channel.sendMessage(text);
                    break;
                }
            }
        }
    }

}
