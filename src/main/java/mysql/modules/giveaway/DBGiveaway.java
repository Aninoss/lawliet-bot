package mysql.modules.giveaway;

import java.util.HashMap;
import java.util.List;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBGiveaway extends DBMapCache<Long, CustomObservableMap<Long, GiveawaySlot>> {

    private static final DBGiveaway ourInstance = new DBGiveaway();

    public static DBGiveaway getInstance() {
        return ourInstance;
    }

    private DBGiveaway() {
    }

    @Override
    protected CustomObservableMap<Long, GiveawaySlot> load(Long guildId) {
        HashMap<Long, GiveawaySlot> giveawaysMapRaw = new DBDataLoad<GiveawaySlot>("Giveaways", "serverId, channelId, messageId, emoji, winners, start, durationMinutes, title, description, imageUrl, active", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getHashMap(
                GiveawaySlot::getMessageId,
                resultSet -> new GiveawaySlot(
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

        CustomObservableMap<Long, GiveawaySlot> giveawaysMap = new CustomObservableMap<>(giveawaysMapRaw);
        giveawaysMap.addMapAddListener(this::addGiveawaySlot)
                .addMapUpdateListener(this::addGiveawaySlot)
                .addMapRemoveListener(this::removeGiveawaySlot);

        return giveawaysMap;
    }

    public List<GiveawaySlot> retrieveAll() {
        return new DBDataLoadAll<GiveawaySlot>("Giveaways", "serverId, channelId, messageId, emoji, winners, start, durationMinutes, title, description, imageUrl, active")
                .getArrayList(
                        resultSet -> new GiveawaySlot(
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

    private void addGiveawaySlot(GiveawaySlot slot) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO Giveaways (serverId, messageId, channelId, emoji, winners, start, durationMinutes, title, description, imageUrl, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, slot.getGuildId());
            preparedStatement.setLong(2, slot.getMessageId());
            preparedStatement.setLong(3, slot.getTextChannelId());
            preparedStatement.setString(4, slot.getEmoji());
            preparedStatement.setInt(5, slot.getWinners());
            preparedStatement.setString(6, DBMain.instantToDateTimeString(slot.getStart()));
            preparedStatement.setLong(7, slot.getDurationMinutes());
            preparedStatement.setString(8, slot.getTitle());
            preparedStatement.setString(9, slot.getDescription());
            preparedStatement.setString(10, slot.getImageUrl().orElse(null));
            preparedStatement.setBoolean(11, slot.isActive());
        });
    }

    private void removeGiveawaySlot(GiveawaySlot slot) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Giveaways WHERE messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, slot.getMessageId());
        });
    }

}
