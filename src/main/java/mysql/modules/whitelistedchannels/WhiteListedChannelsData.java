package mysql.modules.whitelistedchannels;

import java.util.List;
import core.CustomObservableList;
import mysql.DataWithGuild;

public class WhiteListedChannelsData extends DataWithGuild {

    private final CustomObservableList<Long> channelIds;

    public WhiteListedChannelsData(long serverId, List<Long> channelIds) {
        super(serverId);
        this.channelIds = new CustomObservableList<>(channelIds);
    }

    public CustomObservableList<Long> getChannelIds() {
        return channelIds;
    }

    public boolean isWhiteListed(long channelId) {
        return channelIds.size() == 0 || channelIds.contains(channelId);
    }

}
