package modules.repair;

import core.CustomThread;
import core.DiscordApiCollection;
import mysql.modules.autochannel.DBAutoChannel;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.server.Server;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class AutoChannelRepair implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(AutoChannelRepair.class);

    private final DiscordApi api;

    public AutoChannelRepair(DiscordApi api) {
        this.api = api;
    }

    public void start() {
        new CustomThread(this, "autochannel_repair", 1).start();
    }

    @Override
    public void run() {
        try {
            DBAutoChannel.getInstance().getAllChildChannelServerIds().stream()
                    .filter(serverId -> DiscordApiCollection.getInstance().getResponsibleShard(serverId) == api.getCurrentShard())
                    .map(api::getServerById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(server -> {
                        try {
                            deleteEmptyVoiceChannels(server);
                        } catch (ExecutionException e) {
                            LOGGER.error("Could not get bean", e);
                        }
                    });
        } catch (SQLException e) {
            LOGGER.error("Error in auto channel synchronization");
        }
    }

    private void deleteEmptyVoiceChannels(Server server) throws ExecutionException {
        DBAutoChannel.getInstance().getBean(server.getId()).getChildChannelIds().transform(server::getVoiceChannelById, DiscordEntity::getId).stream()
                .filter(vc -> vc.getConnectedUsers().isEmpty())
                .forEach(vc -> vc.delete().exceptionally(ExceptionLogger.get()));
    }

}
