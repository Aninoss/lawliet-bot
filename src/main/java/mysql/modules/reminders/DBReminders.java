package mysql.modules.reminders;

import core.CustomObservableMap;
import mysql.DBCached;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.modules.server.DBServer;
import java.util.HashMap;

public class DBReminders extends DBCached {

    private static final DBReminders ourInstance = new DBReminders();
    public static DBReminders getInstance() { return ourInstance; }
    private DBReminders() {}

    private CustomObservableMap<Integer, RemindersBean> remindersBeans = null;

    public CustomObservableMap<Integer, RemindersBean> loadBean() {
        if (remindersBeans == null) {
            HashMap<Integer, RemindersBean> remindersMap = new DBDataLoad<RemindersBean>("Reminders", "id, serverId, channelId, time, message", "1",
                    preparedStatement -> {}
            ).getHashMap(
                    RemindersBean::getId,
                    resultSet -> {
                        long serverId = resultSet.getLong(2);
                        return new RemindersBean(
                        DBServer.getInstance().getBean(serverId),
                        resultSet.getInt(1),
                        resultSet.getLong(3),
                        resultSet.getTimestamp(4).toInstant(),
                        resultSet.getString(5)
                );
            });

            remindersBeans = new CustomObservableMap<>(remindersMap);
            remindersBeans.addMapAddListener(this::addRemindersBean);
            remindersBeans.addMapRemoveListener(this::removeRemindersBean);
        }

        return remindersBeans;
    }

    private void addRemindersBean(RemindersBean remindersBean) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO Reminders (id, serverId, channelId, time, message) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, remindersBean.getId());
            preparedStatement.setLong(2, remindersBean.getServerId());
            preparedStatement.setLong(3, remindersBean.getChannelId());
            preparedStatement.setString(4, DBMain.instantToDateTimeString(remindersBean.getTime()));
            preparedStatement.setString(5, remindersBean.getMessage());
        });
    }

    private void removeRemindersBean(RemindersBean remindersBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Reminders WHERE id = ?;", preparedStatement -> {
            preparedStatement.setInt(1, remindersBean.getId());
        });
    }

    @Override
    public void clear() {
        remindersBeans = null;
    }

}
