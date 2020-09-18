package mysql.modules.whitelistedchannels;

import core.CustomObservableList;
import mysql.BeanWithServer;
import mysql.modules.server.ServerBean;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;

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
