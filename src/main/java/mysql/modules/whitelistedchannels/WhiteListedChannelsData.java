package mysql.modules.whitelistedchannels;

import core.CustomObservableList;
import core.utils.JDAUtil;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.List;

public class WhiteListedChannelsData extends DataWithGuild {

    private final CustomObservableList<Long> channelIds;

    public WhiteListedChannelsData(long serverId, List<Long> channelIds) {
        super(serverId);
        this.channelIds = new CustomObservableList<>(channelIds);
    }

    public CustomObservableList<Long> getChannelIds() {
        return channelIds;
    }

    public boolean isWhiteListed(GuildMessageChannel channel) {
        return channelIds.isEmpty() || JDAUtil.collectionContainsChannelOrParent(channelIds, channel);
    }

}
