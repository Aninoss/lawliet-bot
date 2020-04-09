package MySQL.Modules.WhiteListedChannels;

import Core.CustomObservableList;
import Core.DiscordApiCollection;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class WhiteListedChannelsBean extends Observable {

    private long serverId;
    private ServerBean serverBean;
    private CustomObservableList<Long> channelIds;

    public WhiteListedChannelsBean(long serverId, ServerBean serverBean, @NonNull ArrayList<Long> channelIds) {
        this.serverId = serverId;
        this.serverBean = serverBean;
        this.channelIds = new CustomObservableList<>(channelIds);
    }


    /* Getters */

    public long getServerId() {
        return serverId;
    }

    public Optional<Server> getServer() { return DiscordApiCollection.getInstance().getServerById(serverId); }

    public ServerBean getServerBean() {
        return serverBean;
    }

    public CustomObservableList<Long> getChannelIds() {
        return channelIds;
    }

    public boolean isWhiteListed(long channelId) { return channelIds.size() == 0 || channelIds.contains(channelId); }

}
