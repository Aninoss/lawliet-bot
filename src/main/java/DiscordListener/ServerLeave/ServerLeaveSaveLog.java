package DiscordListener.ServerLeave;

import CommandSupporters.CommandLogger.CommandLogger;
import Constants.Settings;
import Core.DiscordApiCollection;
import Core.TextManager;
import DiscordListener.DiscordListenerAnnotation;
import DiscordListener.ListenerTypeAbstracts.ServerLeaveAbstract;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@DiscordListenerAnnotation
public class ServerLeaveSaveLog extends ServerLeaveAbstract {

    @Override
    public boolean onServerLeave(ServerLeaveEvent event) throws Throwable {
        if (event.getServer().getMemberCount() >= 100) {
            CommandLogger.getInstance().saveLog(event.getServer().getId(), true);
        }

        return true;
    }

}
