package MySQL;

import org.javacord.api.entity.server.Server;

public class DBVirtualViews {
    public static String getPowerPlantUsersRanks(Server server) {
        String str =  "SELECT\n" +
                "ppRanksA.serverId,\n" +
                "ppRanksA.userId,\n" +
                "COUNT(IF(ppRanksB.growth > ppRanksA.growth OR (ppRanksB.growth = ppRanksA.growth AND ppRanksB.joule > ppRanksA.joule) OR (ppRanksB.growth = ppRanksA.growth AND ppRanksB.joule = ppRanksA.joule AND ppRanksB.coins > ppRanksA.coins), 1, NULL)) rank,\n" +
                "ppRanksA.growth,\n" +
                "ppRanksA.coins,\n" +
                "ppRanksA.joule,\n" +
                "ppRanksA.dailyStreak\n" +
                "FROM (SELECT * FROM PowerPlantUsersExtended WHERE serverId = %s) ppRanksA, (SELECT * FROM PowerPlantUsersExtended WHERE serverId = %s) ppRanksB\n" +
                "WHERE ppRanksB.serverId = ppRanksA.serverId AND ppRanksB.onServer = 1 AND ppRanksA.onServer = 1\n" +
                "GROUP BY ppRanksA.serverId, ppRanksA.userId  \n" +
                "ORDER BY rank ASC";

        return str.replace("%s", server.getIdAsString());
    }
}
