package MySQL.Modules.WhiteListedChannels;

import Core.CustomObservableList;
import Core.DiscordApiCollection;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.javacord.api.entity.server.Server;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Optional;

public class WhiteListedChannelsBean extends BeanWithServer {

    private final CustomObservableList<Long> channelIds;

    public WhiteListedChannelsBean(ServerBean serverBean, @NonNull ArrayList<Long> channelIds) {
        super(serverBean);
        this.channelIds = new CustomObservableList<>(channelIds);
    }


    /* Getters */

    public CustomObservableList<Long> getChannelIds() {
        return channelIds;
    }

    public boolean isWhiteListed(long channelId) { return channelIds.size() == 0 || channelIds.contains(channelId); }

}
