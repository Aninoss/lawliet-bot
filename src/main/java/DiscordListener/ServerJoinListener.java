package DiscordListener;

import Constants.PowerPlantStatus;
import Constants.Settings;
import General.Connector;
import General.DiscordApiCollection;
import MySQL.DBBot;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.ServerJoinEvent;
import org.javacord.api.event.server.ServerLeaveEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class ServerJoinListener {
    public ServerJoinListener() {}

    public void onServerJoin(ServerJoinEvent event) {
        try {
            DBServer.insertServer(event.getServer());
            DBUser.insertUsers(event.getServer().getMembers());
            sendNewMessage(event.getServer());
            DiscordApiCollection.getInstance().getOwner().sendMessage("**+++** "+event.getServer().getName() + " (" + event.getServer().getMembers().size() + ")");

            Connector.updateActivity();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendNewMessage(Server server) {
        String text = "Hi! Thanks for inviting me to your lovely server! <3\n\nJust write `L.help` to get an overview of all my commands.\nIf these commands should only work in certain channels, run `L.whitelist`\nWith `L.fishery` you can configure the tactical \"level system\" game and read how it works.\nFurthermore, if you wanna switch the language to German, write `L.language de`\n\nAnd finally, if you need any help, just join the Lawliet Support Server:\n"+ Settings.SERVER_INVITE_URL;

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
