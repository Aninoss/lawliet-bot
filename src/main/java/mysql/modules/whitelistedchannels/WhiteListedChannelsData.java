package mysql.modules.whitelistedchannels;

import java.util.ArrayList;
import core.CustomObservableList;
import mysql.DataWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class WhiteListedChannelsData extends DataWithGuild {

    private final CustomObservableList<Long> channelIds;

    public WhiteListedChannelsData(long serverId, @NonNull ArrayList<Long> channelIds) {
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
