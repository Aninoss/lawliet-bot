package mysql.modules.reminders;

import java.util.HashMap;
import core.CustomObservableMap;
import core.ShardManager;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleCache;

public class DBReminders extends DBSingleCache<CustomObservableMap<Integer, RemindersBean>> {

    private static final DBReminders ourInstance = new DBReminders();

    public static DBReminders getInstance() {
        return ourInstance;
    }

    private DBReminders() {
    }

    @Override
    protected CustomObservableMap<Integer, RemindersBean> loadBean() {
        HashMap<Integer, RemindersBean> remindersMap = new DBDataLoad<RemindersBean>("Reminders", "id, serverId, channelId, time, message", "(serverId >> 22) % ? >= ? AND (serverId >> 22) % ? <= ?",
                preparedStatement -> {
                    preparedStatement.setInt(1, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(2, ShardManager.getInstance().getShardIntervalMin());
                    preparedStatement.setInt(3, ShardManager.getInstance().getTotalShards());
                    preparedStatement.setInt(4, ShardManager.getInstance().getShardIntervalMax());
                }
        ).getHashMap(
                RemindersBean::getId,
                resultSet -> {
                    long serverId = resultSet.getLong(2);
                    return new RemindersBean(
                            serverId,
                            resultSet.getInt(1),
                            resultSet.getLong(3),
                            resultSet.getTimestamp(4).toInstant(),
                            resultSet.getString(5)
                    );
                }
        );

        CustomObservableMap<Integer, RemindersBean> remindersBeans = new CustomObservableMap<>(remindersMap);
        remindersBeans.addMapAddListener(this::addRemindersBean);
        remindersBeans.addMapRemoveListener(this::removeRemindersBean);

        return remindersBeans;
    }

    private void addRemindersBean(RemindersBean remindersBean) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO Reminders (id, serverId, channelId, time, message) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, remindersBean.getId());
            preparedStatement.setLong(2, remindersBean.getGuildId());
            preparedStatement.setLong(3, remindersBean.getTextChannelId());
            preparedStatement.setString(4, DBMain.instantToDateTimeString(remindersBean.getTime()));
            preparedStatement.setString(5, remindersBean.getMessage());
        });
    }

    private void removeRemindersBean(RemindersBean remindersBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Reminders WHERE id = ?;", preparedStatement -> {
            preparedStatement.setInt(1, remindersBean.getId());
        });
    }

}
