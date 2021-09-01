package mysql.modules.giveaway;

import java.util.List;
import java.util.Map;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMapCache;
import mysql.MySQLManager;

public class DBGiveaway extends DBMapCache<Long, CustomObservableMap<Long, GiveawayData>> {

    private static final DBGiveaway ourInstance = new DBGiveaway();

    public static DBGiveaway getInstance() {
        return ourInstance;
    }

    private DBGiveaway() {
    }

    @Override
    protected CustomObservableMap<Long, GiveawayData> load(Long guildId) {
        Map<Long, GiveawayData> giveawaysMapRaw = new DBDataLoad<GiveawayData>("Giveaways", "serverId, channelId, messageId, emoji, winners, start, durationMinutes, title, description, imageUrl, active", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getMap(
                GiveawayData::getMessageId,
                resultSet -> new GiveawayData(
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getLong(3),
                        resultSet.getString(4),
                        resultSet.getInt(5),
                        resultSet.getTimestamp(6).toInstant(),
                        resultSet.getLong(7),
                        resultSet.getString(8),
                        resultSet.getString(9),
                        resultSet.getString(10),
                        resultSet.getBoolean(11)
                )
        );

        CustomObservableMap<Long, GiveawayData> giveawaysMap = new CustomObservableMap<>(giveawaysMapRaw);
        giveawaysMap.addMapAddListener(this::addGiveawaySlot)
                .addMapUpdateListener(this::addGiveawaySlot)
                .addMapRemoveListener(this::removeGiveawaySlot);

        return giveawaysMap;
    }

    public List<GiveawayData> retrieveAll() {
        return new DBDataLoadAll<GiveawayData>("Giveaways", "serverId, channelId, messageId, emoji, winners, start, durationMinutes, title, description, imageUrl, active")
                .getList(
                        resultSet -> new GiveawayData(
                                resultSet.getLong(1),
                                resultSet.getLong(2),
                                resultSet.getLong(3),
                                resultSet.getString(4),
                                resultSet.getInt(5),
                                resultSet.getTimestamp(6).toInstant(),
                                resultSet.getLong(7),
                                resultSet.getString(8),
                                resultSet.getString(9),
                                resultSet.getString(10),
                                resultSet.getBoolean(11)
                        )
                );
    }

    private void addGiveawaySlot(GiveawayData slot) {
        MySQLManager.asyncUpdate("REPLACE INTO Giveaways (serverId, messageId, channelId, emoji, winners, start, durationMinutes, title, description, imageUrl, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, slot.getGuildId());
            preparedStatement.setLong(2, slot.getMessageId());
            preparedStatement.setLong(3, slot.getTextChannelId());
            preparedStatement.setString(4, slot.getEmoji());
            preparedStatement.setInt(5, slot.getWinners());
            preparedStatement.setString(6, MySQLManager.instantToDateTimeString(slot.getStart()));
            preparedStatement.setLong(7, slot.getDurationMinutes());
            preparedStatement.setString(8, slot.getTitle());
            preparedStatement.setString(9, slot.getDescription());
            preparedStatement.setString(10, slot.getImageUrl().orElse(null));
            preparedStatement.setBoolean(11, slot.isActive());
        });
    }

    private void removeGiveawaySlot(GiveawayData slot) {
        MySQLManager.asyncUpdate("DELETE FROM Giveaways WHERE messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, slot.getMessageId());
        });
    }

}
