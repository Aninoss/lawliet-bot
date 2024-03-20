package core.atomicassets;

import core.CustomObservableList;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AtomicVoiceChannel extends AbstractAtomicGuildChannel<VoiceChannel> {

    public AtomicVoiceChannel(long guildId, long channelId) {
        super(VoiceChannel.class, guildId, channelId);
    }

    public AtomicVoiceChannel(VoiceChannel channel) {
        super(VoiceChannel.class, channel.getGuild().getIdLong(), channel.getIdLong());
    }

    public static List<AtomicVoiceChannel> from(List<VoiceChannel> channels) {
        return channels.stream()
                .map(AtomicVoiceChannel::new)
                .collect(Collectors.toList());
    }

    public static List<VoiceChannel> to(List<AtomicVoiceChannel> channels) {
        return channels.stream()
                .map(AtomicVoiceChannel::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public static CustomObservableList<AtomicVoiceChannel> transformIdList(Guild guild, CustomObservableList<Long> list) {
        return list.transform(
                id -> new AtomicVoiceChannel(guild.getIdLong(), id),
                AtomicVoiceChannel::getIdLong
        );
    }

}
