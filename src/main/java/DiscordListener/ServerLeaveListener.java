package DiscordListener;

import General.Connector;
import General.DiscordApiCollection;
import MySQL.DBServer;
import org.javacord.api.event.server.ServerLeaveEvent;

import java.util.concurrent.ExecutionException;

public class ServerLeaveListener {

    public void onServerLeave(ServerLeaveEvent event) {
        if (event.getServer().getMembers().size() >= 100) DiscordApiCollection.getInstance().getOwner().sendMessage("**---** "+event.getServer().getName() + " (" + event.getServer().getMembers().size() + ")");
    }
}
