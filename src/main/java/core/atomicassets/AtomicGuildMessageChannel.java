package core.atomicassets;

import core.CustomObservableList;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtomicGuildMessageChannel extends AbstractAtomicGuildChannel<GuildMessageChannel> {

    public AtomicGuildMessageChannel(long guildId, long channelId) {
        super(GuildMessageChannel.class, guildId, channelId);
    }

    public AtomicGuildMessageChannel(GuildMessageChannel channel) {
        super(GuildMessageChannel.class, channel.getGuild().getIdLong(), channel.getIdLong());
    }

    public static List<AtomicGuildMessageChannel> from(List<GuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicGuildMessageChannel::new)
                .collect(Collectors.toList());
    }

    public static List<GuildMessageChannel> to(List<AtomicGuildMessageChannel> channels) {
        return channels.stream()
                .map(AtomicGuildMessageChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicGuildMessageChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicGuildMessageChannel(guild.getIdLong(), id),
                AtomicGuildMessageChannel::getIdLong
        );
    }

}
