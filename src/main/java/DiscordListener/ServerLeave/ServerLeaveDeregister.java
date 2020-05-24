package DiscordListener.ServerLeave;

import CommandSupporters.CommandLogger.CommandLogger;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerPriority;
import DiscordListener.ListenerTypeAbstracts.ServerLeaveAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.ServerLeaveEvent;

@DiscordListenerAnnotation(priority = ListenerPriority.LOW)
public class ServerLeaveDeregister extends ServerLeaveAbstract {

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        DBServer.getInstance().remove(event.getServer().getId());
        return true;
    }

}
