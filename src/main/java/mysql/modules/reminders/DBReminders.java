package mysql.modules.reminders;

import java.util.HashMap;
import java.util.List;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBReminders extends DBMapCache<Long, CustomObservableMap<Long, ReminderData>> {

    private static final DBReminders ourInstance = new DBReminders();

    public static DBReminders getInstance() {
        return ourInstance;
    }

    private DBReminders() {
    }

    @Override
    protected CustomObservableMap<Long, ReminderData> load(Long guildId) throws Exception {
        HashMap<Long, ReminderData> remindersMap = new DBDataLoad<ReminderData>("Reminders", "id, serverId, channelId, time, message, messageId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getHashMap(
                ReminderData::getId,
                resultSet -> {
                    long serverId = resultSet.getLong(2);
                    return new ReminderData(
                            serverId,
                            resultSet.getLong(1),
                            resultSet.getLong(3),
                            resultSet.getLong(6),
                            resultSet.getTimestamp(4).toInstant(),
                            resultSet.getString(5)
                    );
                }
        );

        CustomObservableMap<Long, ReminderData> remindersBeans = new CustomObservableMap<>(remindersMap);
        remindersBeans.addMapAddListener(this::addRemindersBean)
                .addMapUpdateListener(this::addRemindersBean)
                .addMapRemoveListener(this::removeRemindersBean);

        return remindersBeans;
    }

    public List<ReminderData> retrieveAll() {
        return new DBDataLoadAll<ReminderData>("Reminders", "id, serverId, channelId, time, message, messageId")
                .getArrayList(
                        resultSet -> {
                            long serverId = resultSet.getLong(2);
                            return new ReminderData(
                                    serverId,
                                    resultSet.getLong(1),
                                    resultSet.getLong(3),
                                    resultSet.getLong(6),
                                    resultSet.getTimestamp(4).toInstant(),
                                    resultSet.getString(5)
                            );
                        }
                );
    }

    private void addRemindersBean(ReminderData remindersBean) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO Reminders (id, serverId, channelId, time, message, messageId) VALUES (?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, remindersBean.getId());
            preparedStatement.setLong(2, remindersBean.getGuildId());
            preparedStatement.setLong(3, remindersBean.getTextChannelId());
            preparedStatement.setString(4, DBMain.instantToDateTimeString(remindersBean.getTime()));
            preparedStatement.setString(5, remindersBean.getMessage());
            preparedStatement.setLong(6, remindersBean.getMessageId());
        });
    }

    private void removeRemindersBean(ReminderData remindersBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Reminders WHERE id = ?;", preparedStatement -> {
            preparedStatement.setLong(1, remindersBean.getId());
        });
    }

}
