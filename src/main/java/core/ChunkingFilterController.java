package core;

import constants.AssetIds;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
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
                (!DBStickyRoles.getInstance().retrieve(guildId).getRoleIds().isEmpty() && !guildIsBig(guildId));
    }

    private boolean guildIsBig(long guildId) {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager()) {
            return entityManager.findGuildEntity(guildId).getBig();
        }
    }

}
