package core.assets;

import net.dv8tion.jda.api.entities.Member;

import java.util.Optional;

public interface MemberAsset extends GuildAsset {

    long getMemberId();

    default Optional<Member> getMember() {
        return getGuild().map(guild -> guild.getMemberById(getMemberId()));
    }

}
