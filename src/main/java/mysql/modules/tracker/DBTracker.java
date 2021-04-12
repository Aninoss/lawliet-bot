package mysql.modules.tracker;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import core.CustomObservableMap;
import mysql.DBDataLoad;
import mysql.DBDataLoadAll;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBTracker extends DBMapCache<Long, CustomObservableMap<Integer, TrackerSlot>> {

    private static final DBTracker ourInstance = new DBTracker();

    public static DBTracker getInstance() {
        return ourInstance;
    }

    private DBTracker() {
    }

    @Override
    protected CustomObservableMap<Integer, TrackerSlot> load(Long guildId) {
        HashMap<Integer, TrackerSlot> trackersMap = new DBDataLoad<TrackerSlot>("Tracking", "serverId, channelId, command, messageId, commandKey, time, arg, webhookUrl, userMessage", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, guildId)
        ).getHashMap(
                TrackerSlot::hashCode,
                resultSet -> new TrackerSlot(
                        resultSet.getLong(1),
                        resultSet.getLong(2),
                        resultSet.getString(3),
                        resultSet.getLong(4),
                        resultSet.getString(5),
                        resultSet.getTimestamp(6).toInstant(),
                        resultSet.getString(7),
                        resultSet.getString(8),
                        resultSet.getString(9)
                )
        );

        CustomObservableMap<Integer, TrackerSlot> remindersBeans = new CustomObservableMap<>(trackersMap);
        remindersBeans.addMapAddListener(this::addTracker)
                .addMapUpdateListener(this::addTracker)
                .addMapRemoveListener(this::removeTracker);

        return remindersBeans;
    }

    public List<TrackerSlot> retrieveAll() {
        return new DBDataLoadAll<TrackerSlot>("Tracking", "serverId, channelId, command, messageId, commandKey, time, arg, webhookUrl, userMessage")
                .getArrayList(
                        resultSet -> new TrackerSlot(
                                resultSet.getLong(1),
                                resultSet.getLong(2),
                                resultSet.getString(3),
                                resultSet.getLong(4),
                                resultSet.getString(5),
                                resultSet.getTimestamp(6).toInstant(),
                                resultSet.getString(7),
                                resultSet.getString(8),
                                resultSet.getString(9)
                        )
                );
    }

    private void addTracker(TrackerSlot slot) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO Tracking (serverId, channelId, command, messageId, commandKey, time, arg, webhookUrl, userMessage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, slot.getGuildId());
            preparedStatement.setLong(2, slot.getTextChannelId());
            preparedStatement.setString(3, slot.getCommandTrigger());

            Optional<Long> messageIdOpt = slot.getMessageId();
            if (messageIdOpt.isPresent()) {
                preparedStatement.setLong(4, messageIdOpt.get());
            } else {
                preparedStatement.setNull(4, Types.BIGINT);
            }

            preparedStatement.setString(5, slot.getCommandKey());
            preparedStatement.setString(6, DBMain.instantToDateTimeString(slot.getNextRequest()));

            Optional<String> argsOpt = slot.getArgs();
            if (argsOpt.isPresent()) {
                preparedStatement.setString(7, argsOpt.get());
            } else {
                preparedStatement.setNull(7, Types.VARCHAR);
            }

            Optional<String> webhookOpt = slot.getWebhookUrl();
            if (webhookOpt.isPresent()) {
                preparedStatement.setString(8, webhookOpt.get());
            } else {
                preparedStatement.setNull(8, Types.VARCHAR);
            }

            Optional<String> userMessageOpt = slot.getUserMessage();
            if (userMessageOpt.isPresent()) {
                preparedStatement.setString(9, userMessageOpt.get());
            } else {
                preparedStatement.setNull(9, Types.VARCHAR);
            }
        });
    }

    private void removeTracker(TrackerSlot slot) {
        if (!Objects.isNull(slot)) {
            DBMain.getInstance().asyncUpdate("DELETE FROM Tracking WHERE channelId = ? AND command = ? AND commandKey = ?;", preparedStatement -> {
                preparedStatement.setLong(1, slot.getTextChannelId());
                preparedStatement.setString(2, slot.getCommandTrigger());
                preparedStatement.setString(3, slot.getCommandKey());
            });
        }
    }

}
