package mysql.modules.whitelistedchannels;

import java.util.ArrayList;
import core.CustomObservableList;
import mysql.BeanWithGuild;
import org.checkerframework.checker.nullness.qual.NonNull;

public class WhiteListedChannelsBean extends BeanWithGuild {

    private final CustomObservableList<Long> channelIds;

    public WhiteListedChannelsBean(long serverId, @NonNull ArrayList<Long> channelIds) {
        super(serverId);
        this.channelIds = new CustomObservableList<>(channelIds);
    }


    /* Getters */

    public CustomObservableList<Long> getChannelIds() {
        return channelIds;
    }

    public boolean isWhiteListed(long channelId) {
        return channelIds.size() == 0 || channelIds.contains(channelId);
    }

}
