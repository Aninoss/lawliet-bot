package core;

import constants.AssetIds;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.utils.ChunkingFilter;

import java.util.HashSet;

public class ChunkingFilterController implements ChunkingFilter {

    private static final ChunkingFilterController ourInstance = new ChunkingFilterController();

    public static ChunkingFilterController getInstance() {
        return ourInstance;
    }

    private final HashSet<Long> hasStickyRolesSet = new HashSet<>();

    private ChunkingFilterController() {
    }

    @Override
    public boolean filter(long guildId) {
        return !Program.publicInstance() ||
                guildId == AssetIds.SUPPORT_SERVER_ID ||
                guildId == AssetIds.ANICORD_SERVER_ID ||
                (hasStickyRoles(guildId) && allowChunking(guildId));
    }

    public void updateStickyRolesCache(GuildEntity guildEntity) {
        if (guildEntity.getStickyRoles().getRoleIds().isEmpty()) {
            hasStickyRolesSet.remove(guildEntity.getGuildId());
        } else {
            hasStickyRolesSet.add(guildEntity.getGuildId());
        }
    }

    private boolean hasStickyRoles(long guildId) {
        return hasStickyRolesSet.contains(guildId);
    }

    private boolean allowChunking(long guildId) {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getMemberCount() < MemberCacheController.BIG_SERVER_THRESHOLD)
                .orElse(false);
    }

}
