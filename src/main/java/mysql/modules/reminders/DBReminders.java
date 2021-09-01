package mysql.modules.reminders;

import java.util.List;
import java.util.Map;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMapCache;
import mysql.MySQLManager;

public class DBReminders extends DBMapCache<Long, CustomObservableMap<Long, ReminderData>> {

    private static final DBReminders ourInstance = new DBReminders();

    public static DBReminders getInstance() {
        return ourInstance;
    }

    private DBReminders() {
    }

    @Override
    protected CustomObservableMap<Long, ReminderData> load(Long guildId) throws Exception {
        Map<Long, ReminderData> remindersMap = new DBDataLoad<ReminderData>("Reminders", "id, serverId, sourceChannelId, channelId, time, message, messageId", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getMap(
                ReminderData::getId,
                resultSet -> {
                    long serverId = resultSet.getLong(2);
                    return new ReminderData(
                            serverId,
                            resultSet.getLong(1),
                            resultSet.getLong(3),
                            resultSet.getLong(4),
                            resultSet.getLong(7),
                            resultSet.getTimestamp(5).toInstant(),
                            resultSet.getString(6)
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
        return new DBDataLoadAll<ReminderData>("Reminders", "id, serverId, sourceChannelId, channelId, time, message, messageId")
                .getList(
                        resultSet -> {
                            long serverId = resultSet.getLong(2);
                            return new ReminderData(
                                    serverId,
                                    resultSet.getLong(1),
                                    resultSet.getLong(3),
                                    resultSet.getLong(4),
                                    resultSet.getLong(7),
                                    resultSet.getTimestamp(5).toInstant(),
                                    resultSet.getString(6)
                            );
                        }
                );
    }

    private void addRemindersBean(ReminderData remindersBean) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO Reminders (id, serverId, sourceChannelId, channelId, time, message, messageId) VALUES (?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, remindersBean.getId());
            preparedStatement.setLong(2, remindersBean.getGuildId());
            preparedStatement.setLong(3, remindersBean.getSourceChannelId());
            preparedStatement.setLong(4, remindersBean.getTargetChannelId());
            preparedStatement.setString(5, MySQLManager.instantToDateTimeString(remindersBean.getTime()));
            preparedStatement.setString(6, remindersBean.getMessage());
            preparedStatement.setLong(7, remindersBean.getMessageId());
        });
    }

    private void removeRemindersBean(ReminderData remindersBean) {
        MySQLManager.asyncUpdate("DELETE FROM Reminders WHERE id = ?;", preparedStatement -> {
            preparedStatement.setLong(1, remindersBean.getId());
        });
    }

}
