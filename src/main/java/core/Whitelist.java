package core;

import core.utils.JDAUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.entities.channel.Channel;

import java.util.List;

public class Whitelist {

    public static boolean isWhitelisted(GuildEntity guildEntity, Channel channel) {
        List<Long> whitelistedChannelIds = guildEntity.getWhitelistedChannelIds();
        return whitelistedChannelIds.isEmpty() || JDAUtil.collectionContainsChannelOrParent(whitelistedChannelIds, channel);
    }

}
