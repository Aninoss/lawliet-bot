package modules.repair;

import commands.runnables.utilitycategory.AutoChannelCommand;
import constants.Permission;
import core.DiscordApiManager;
import core.PermissionCheckRuntime;
import mysql.modules.autochannel.AutoChannelBean;
import mysql.modules.autochannel.DBAutoChannel;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoChannelRepair {

    private final static Logger LOGGER = LoggerFactory.getLogger(AutoChannelRepair.class);

    private static final AutoChannelRepair ourInstance = new AutoChannelRepair();
    public static AutoChannelRepair getInstance() { return ourInstance; }
    private AutoChannelRepair() { }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void start(DiscordApi api) {
        executorService.submit(() -> run(api));
    }

    public void run(DiscordApi api) {
        try {
            DBAutoChannel.getInstance().getAllChildChannelServerIds().stream()
                    .filter(serverId -> DiscordApiManager.getInstance().getResponsibleShard(serverId) == api.getCurrentShard())
                    .map(api::getServerById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(this::deleteEmptyVoiceChannels);
        } catch (SQLException e) {
            LOGGER.error("Error in auto channel synchronization");
        }
    }

    private void deleteEmptyVoiceChannels(Server server) {
        AutoChannelBean autoChannelBean = DBAutoChannel.getInstance().getBean(server.getId());
        autoChannelBean.getChildChannelIds().transform(server::getVoiceChannelById, DiscordEntity::getId).stream()
                .filter(vc -> vc.getConnectedUsers().isEmpty() && PermissionCheckRuntime.getInstance().botHasPermission(autoChannelBean.getServerBean().getLocale(), AutoChannelCommand.class, vc, Permission.MANAGE_CHANNEL | Permission.CONNECT))
                .forEach(ServerChannel::delete); //no log
    }

}
