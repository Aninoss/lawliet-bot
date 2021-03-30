package mysql.modules.reminders;

import java.util.HashMap;
import java.util.List;
import core.CustomObservableMap;
import core.ShardManager;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBReminders extends DBMapCache<Long, CustomObservableMap<Long, ReminderSlot>> {

    private static final DBReminders ourInstance = new DBReminders();

    public static DBReminders getInstance() {
        return ourInstance;
    }

    private DBReminders() {
    }

    @Override
    protected CustomObservableMap<Long, ReminderSlot> load(Long guildId) throws Exception {
        HashMap<Long, ReminderSlot> remindersMap = new DBDataLoad<ReminderSlot>("Reminders", "id, serverId, channelId, time, message", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getHashMap(
                ReminderSlot::getId,
                resultSet -> {
                    long serverId = resultSet.getLong(2);
                    return new ReminderSlot(
                            serverId,
                            resultSet.getLong(1),
                            resultSet.getLong(3),
                            resultSet.getTimestamp(4).toInstant(),
                            resultSet.getString(5)
                    );
                }
        );

        CustomObservableMap<Long, ReminderSlot> remindersBeans = new CustomObservableMap<>(remindersMap);
        remindersBeans.addMapAddListener(this::addRemindersBean)
                .addMapUpdateListener(this::addRemindersBean)
                .addMapRemoveListener(this::removeRemindersBean);

        return remindersBeans;
    }

    public List<ReminderSlot> retrieveAll() {
        return new DBDataLoad<ReminderSlot>("Reminders", "id, serverId, channelId, time, message", "(serverId >> 22) % ? >= ? AND (serverId >> 22) % ? <= ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(2, ShardManager.getInstance().getShardIntervalMin());
                    preparedStatement.setInt(3, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(4, ShardManager.getInstance().getShardIntervalMax());
                }
        ).getArrayList(
                resultSet -> {
                    long serverId = resultSet.getLong(2);
                    return new ReminderSlot(
                            serverId,
                            resultSet.getInt(1),
                            resultSet.getLong(3),
                            resultSet.getTimestamp(4).toInstant(),
                            resultSet.getString(5)
                    );
                }
        );
    }

    private void addRemindersBean(ReminderSlot remindersBean) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO Reminders (id, serverId, channelId, time, message) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, remindersBean.getId());
            preparedStatement.setLong(2, remindersBean.getGuildId());
            preparedStatement.setLong(3, remindersBean.getTextChannelId());
            preparedStatement.setString(4, DBMain.instantToDateTimeString(remindersBean.getTime()));
            preparedStatement.setString(5, remindersBean.getMessage());
        });
    }

    private void removeRemindersBean(ReminderSlot remindersBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Reminders WHERE id = ?;", preparedStatement -> {
            preparedStatement.setLong(1, remindersBean.getId());
        });
    }

}
