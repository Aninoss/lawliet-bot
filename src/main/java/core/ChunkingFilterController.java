package core;

import constants.AssetIds;
import mysql.modules.stickyroles.DBStickyRoles;
import net.dv8tion.jda.api.utils.ChunkingFilter;

public class ChunkingFilterController implements ChunkingFilter {

    private static final ChunkingFilterController ourInstance = new ChunkingFilterController();

    public static ChunkingFilterController getInstance() {
        return ourInstance;
    }

    private ChunkingFilterController() {
    }

    @Override
    public boolean filter(long guildId) {
        return !Program.publicVersion() ||
                guildId == AssetIds.SUPPORT_SERVER_ID ||
                guildId == AssetIds.ANICORD_SERVER_ID ||
                (!DBStickyRoles.getInstance().retrieve(guildId).getRoleIds().isEmpty() && allowChunking(guildId));
    }

    public boolean allowChunking(long guildId) {
        return ShardManager.getLocalGuildById(guildId)
                .map(guild -> guild.getMemberCount() < MemberCacheController.BIG_SERVER_THRESHOLD)
                .orElse(false);
    }

}
