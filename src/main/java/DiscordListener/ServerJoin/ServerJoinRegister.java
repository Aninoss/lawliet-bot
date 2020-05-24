package DiscordListener.ServerJoin;

import Core.DiscordApiCollection;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerPriority;
import DiscordListener.ListenerTypeAbstracts.ServerJoinAbstract;
import MySQL.Modules.Server.DBServer;
import org.javacord.api.event.server.ServerJoinEvent;

@DiscordListenerAnnotation(priority = ListenerPriority.HIGH)
public class ServerJoinRegister extends ServerJoinAbstract {

    @Override
    public boolean onServerJoin(ServerJoinEvent event) throws Throwable {
        DBServer.getInstance().getBean(event.getServer().getId());
        return true;
    }

}
