package mysql.modules.casinostats;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import mysql.DBDataLoad;
import mysql.DBMapCache;
import mysql.MySQLManager;

public class DBCasinoStats extends DBMapCache<DBCasinoStats.Key, CasinoStatsData> {

    private static final DBCasinoStats ourInstance = new DBCasinoStats();

    public static DBCasinoStats getInstance() {
        return ourInstance;
    }

    private DBCasinoStats() {
    }

    @Override
    protected CasinoStatsData load(DBCasinoStats.Key key) throws Exception {
        List<CasinoStatsSlot> casinoStatsList = new DBDataLoad<CasinoStatsSlot>("CasinoStats", "id, game, won, value", "serverId = ? AND userId = ?",
                preparedStatement -> {
                    preparedStatement.setLong(1, key.guildId);
                    preparedStatement.setLong(2, key.userId);
                }
        ).getList(
                resultSet -> new CasinoStatsSlot(
                        resultSet.getString(1),
                        resultSet.getString(2),
                        resultSet.getBoolean(3),
                        resultSet.getLong(4)
                )
        );

        CasinoStatsData casinoStatsData = new CasinoStatsData(casinoStatsList);
        casinoStatsData.getCasinoStatsList()
                .addListAddListener(list -> list.forEach(slot -> addSlot(slot, key)));

        return casinoStatsData;
    }

    private void addSlot(CasinoStatsSlot casinoStatsSlot, DBCasinoStats.Key key) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO CasinoStats (id, serverId, userId, game, won, value) VALUES (?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setString(1, casinoStatsSlot.getId());
            preparedStatement.setLong(2, key.guildId);
            preparedStatement.setLong(3, key.userId);
            preparedStatement.setString(4, casinoStatsSlot.getGame());
            preparedStatement.setBoolean(5, casinoStatsSlot.isWon());
            preparedStatement.setLong(6, casinoStatsSlot.getValue());
        });
    }

    public void removeMember(long guildId, long userId) {
        MySQLManager.asyncUpdate("DELETE FROM CasinoStats WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, guildId);
            preparedStatement.setLong(2, userId);
        });
        getCache().put(new Key(guildId, userId), new CasinoStatsData(Collections.emptyList()));
    }


    public static class Key {

        private final long guildId;
        private final long userId;

        public Key(long guildId, long userId) {
            this.guildId = guildId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return guildId == key.guildId && userId == key.userId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(guildId, userId);
        }

    }

}
