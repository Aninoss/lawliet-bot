package core.atomicassets;

import core.DiscordApiManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Optional;

public class AtomicGuild implements AtomicAsset<Guild> {

    private final long guildId;

    public AtomicGuild(Guild guild) {
        guildId = guild.getIdLong();
    }

    @Override
    public long getId() {
        return guildId;
    }

    @Override
    public Optional<Guild> get() {
        return DiscordApiManager.getInstance().getLocalGuildById(guildId);
    }

}
