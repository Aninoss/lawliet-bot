package mysql.modules.giveaway;

import core.CustomObservableMap;
import core.DiscordApiManager;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleBeanGenerator;

import java.util.HashMap;

public class DBGiveaway extends DBSingleBeanGenerator<CustomObservableMap<Long, GiveawayBean>> {

    private static final DBGiveaway ourInstance = new DBGiveaway();

    public static DBGiveaway getInstance() {
        return ourInstance;
    }

    private DBGiveaway() {
    }

    @Override
    protected CustomObservableMap<Long, GiveawayBean> loadBean() {
        HashMap<Long, GiveawayBean> giveawaysMap = new DBDataLoad<GiveawayBean>("Giveaways", "serverId, channelId, messageId, emoji, winners, start, durationMinutes, title, description, imageUrl, active", "(serverId >> 22) % ? >= ? AND (serverId >> 22) % ? <= ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, DiscordApiManager.getInstance().getTotalShards());
                    preparedStatement.setInt(2, DiscordApiManager.getInstance().getShardIntervalMin());
                    preparedStatement.setInt(3, DiscordApiManager.getInstance().getTotalShards());
                    preparedStatement.setInt(4, DiscordApiManager.getInstance().getShardIntervalMax());
                }
        ).getHashMap(
                GiveawayBean::getMessageId,
                resultSet -> new GiveawayBean(
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

        CustomObservableMap<Long, GiveawayBean> giveawayBeans = new CustomObservableMap<>(giveawaysMap);
        giveawayBeans.addMapAddListener(this::addGiveawaySlot)
                .addMapUpdateListener(this::addGiveawaySlot)
                .addMapRemoveListener(this::removeGiveawaySlot);

        return giveawayBeans;
    }

    private void removeGiveawaySlot(GiveawayBean slot) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Giveaways WHERE messageId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, slot.getMessageId());
        });
    }

    private void addGiveawaySlot(GiveawayBean slot) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO Giveaways (serverId, messageId, channelId, emoji, winners, start, durationMinutes, title, description, imageUrl, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, slot.getServerId());
            preparedStatement.setLong(2, slot.getMessageId());
            preparedStatement.setLong(3, slot.getChannelId());
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

}
