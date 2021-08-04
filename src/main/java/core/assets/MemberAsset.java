package core.assets;

import java.util.Optional;
import core.MemberCacheController;
import net.dv8tion.jda.api.entities.Member;

public interface MemberAsset extends GuildAsset {

    long getMemberId();

    default Optional<Member> getMember() {
        return getGuild()
                .map(guild -> MemberCacheController.getInstance().loadMember(guild, getMemberId()).join());
    }

}
