package DiscordListener;

import General.Connector;
import MySQL.DBServer;
import org.javacord.api.event.server.ServerLeaveEvent;

import java.util.concurrent.ExecutionException;

public class ServerLeaveListener {
    public ServerLeaveListener() {}

    public void onServerLeave(ServerLeaveEvent event) {
        try {
            event.getApi().getOwner().get().sendMessage("**---** "+event.getServer().getName() + " (" + event.getServer().getMembers().size() + ")");
            Connector.updateActivity(event.getApi());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
