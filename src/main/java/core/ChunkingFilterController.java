package core;

import constants.AssetIds;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.utils.ChunkingFilter;

public class ChunkingFilterController implements ChunkingFilter {

    @Override
    public boolean filter(long guildId) {
        return guildId == AssetIds.SUPPORT_SERVER_ID ||
                guildId == AssetIds.ANICORD_SERVER_ID ||
                DBGuild.getInstance().retrieve(guildId).isBig();
    }

}
