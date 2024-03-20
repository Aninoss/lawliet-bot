package core.atomicassets;

import core.CustomObservableList;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtomicGuildChannel extends AbstractAtomicGuildChannel<GuildChannel> {

    public AtomicGuildChannel(long guildId, long channelId) {
        super(GuildChannel.class, guildId, channelId);
    }

    public AtomicGuildChannel(GuildChannel channel) {
        super(GuildChannel.class, channel.getGuild().getIdLong(), channel.getIdLong());
    }

    public static List<AtomicGuildChannel> from(List<GuildChannel> channels) {
        return channels.stream()
                .map(AtomicGuildChannel::new)
                .collect(Collectors.toList());
    }

    public static List<GuildChannel> to(List<AtomicGuildChannel> channels) {
        return channels.stream()
                .map(AtomicGuildChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicGuildChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicGuildChannel(guild.getIdLong(), id),
                AtomicGuildChannel::getIdLong
        );
    }

}
