package core.atomicassets;

import core.CustomObservableList;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtomicStandardGuildMessageChannel extends AbstractAtomicGuildChannel<StandardGuildMessageChannel> {

    public AtomicStandardGuildMessageChannel(long guildId, long channelId) {
        super(StandardGuildMessageChannel.class, guildId, channelId);
    }

    public AtomicStandardGuildMessageChannel(StandardGuildMessageChannel channel) {
        super(StandardGuildMessageChannel.class, channel.getGuild().getIdLong(), channel.getIdLong());
    }

    public static List<AtomicStandardGuildMessageChannel> from(List<StandardGuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicStandardGuildMessageChannel::new)
                .collect(Collectors.toList());
    }

    public static List<StandardGuildMessageChannel> to(List<AtomicStandardGuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicStandardGuildMessageChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicStandardGuildMessageChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicStandardGuildMessageChannel(guild.getIdLong(), id),
                AtomicStandardGuildMessageChannel::getIdLong
        );
    }

}
