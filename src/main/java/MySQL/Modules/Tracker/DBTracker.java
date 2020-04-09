package MySQL.Modules.Tracker;

import MySQL.DBCached;
import MySQL.DBDataLoad;
import MySQL.DBMain;
import MySQL.Modules.Server.DBServer;
import javafx.util.Pair;

import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DBTracker extends DBCached {

    private static DBTracker ourInstance = new DBTracker();
    public static DBTracker getInstance() { return ourInstance; }
    private DBTracker() {}

    private TrackerBean trackerBean = null;

    public void init() {
        Thread t = new Thread(() -> {
            try {
                getBean();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        t.setName("tracker_init");
        t.setPriority(1);
        t.start();
    }

    public synchronized TrackerBean getBean() throws SQLException {
        if (trackerBean == null) {
            HashMap<Pair<Long, String>, TrackerBeanSlot> slots = new DBDataLoad<TrackerBeanSlot>("Tracking", "serverId, channelId, command, messageId, commandKey, time, arg", "1", preparedStatement -> {})
                    .getHashMap(
                            slot -> new Pair<>(slot.getChannelId(), slot.getCommandTrigger()),
                            resultSet -> {
                                try {
                                    return new TrackerBeanSlot(
                                            resultSet.getLong(1),
                                            DBServer.getInstance().getBean(resultSet.getLong(1)),
                                            resultSet.getLong(2),
                                            resultSet.getString(3),
                                            resultSet.getLong(4),
                                            resultSet.getString(5),
                                            resultSet.getTimestamp(6).toInstant(),
                                            resultSet.getString(7)
                                    );
                                } catch (ExecutionException e) {
                                    //Ignore
                                }
                                return null;
                            }
                    );

            trackerBean = new TrackerBean(slots);
            trackerBean.getMap()
                    .addMapAddListener(this::insertTracker)
                    .addMapUpdateListener(this::insertTracker)
                    .addMapRemoveListener(this::removeTracker);
        }

        return trackerBean;
    }

    protected void insertTracker(TrackerBeanSlot slot) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO Tracking (serverId, channelId, command, messageId, commandKey, time, arg) VALUES (?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, slot.getServerId());
            preparedStatement.setLong(2, slot.getChannelId());
            preparedStatement.setString(3, slot.getCommandTrigger());

            Optional<Long> messageIdOpt = slot.getMessageId();
            if (messageIdOpt.isPresent()) preparedStatement.setLong(4, messageIdOpt.get());
            else preparedStatement.setNull(4, Types.BIGINT);

            Optional<String> commandKeyOpt = slot.getCommandKey();
            if (commandKeyOpt.isPresent()) preparedStatement.setString(5, commandKeyOpt.get());
            else preparedStatement.setNull(5, Types.VARCHAR);

            preparedStatement.setString(6, DBMain.instantToDateTimeString(slot.getNextRequest()));

            Optional<String> argsOpt = slot.getArgs();
            if (argsOpt.isPresent()) preparedStatement.setString(7, argsOpt.get());
            else preparedStatement.setNull(7, Types.VARCHAR);
        });
    }

    protected void removeTracker(TrackerBeanSlot slot) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Tracking WHERE channelId = ? AND command = ?;", preparedStatement -> {
            preparedStatement.setLong(1, slot.getChannelId());
            preparedStatement.setString(2, slot.getCommandTrigger());
        });
    }

    @Override
    public void clear() {
        //Ignore
    }

}
