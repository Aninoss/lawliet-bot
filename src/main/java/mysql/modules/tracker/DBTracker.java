package mysql.modules.tracker;

import core.CustomThread;
import mysql.DBCached;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.modules.server.DBServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class DBTracker extends DBCached {

    private final static Logger LOGGER = LoggerFactory.getLogger(DBTracker.class);

    private static final DBTracker ourInstance = new DBTracker();
    public static DBTracker getInstance() { return ourInstance; }
    private DBTracker() {}

    private TrackerBean trackerBean = null;
    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        Thread t = new CustomThread(() -> {
            try {
                getBean();
            } catch (SQLException e) {
                LOGGER.error("Could not get bean", e);
            }
        }, "tracker_init");
        t.start();
    }

    public synchronized TrackerBean getBean() throws SQLException {
        if (trackerBean == null) {
            ArrayList<TrackerBeanSlot> slots = new DBDataLoad<TrackerBeanSlot>("Tracking", "serverId, channelId, command, messageId, commandKey, time, arg", "1", preparedStatement -> {})
                    .getArrayList(
                            resultSet -> {
                                try {
                                    return new TrackerBeanSlot(
                                            DBServer.getInstance().getBean(resultSet.getLong(1)),
                                            resultSet.getLong(2),
                                            resultSet.getString(3),
                                            resultSet.getLong(4),
                                            resultSet.getString(5),
                                            resultSet.getTimestamp(6).toInstant(),
                                            resultSet.getString(7)
                                    );
                                } catch (ExecutionException e) {
                                    LOGGER.error("Exception when creating tracker bean", e);
                                }
                                return null;
                            }
                    );

            trackerBean = new TrackerBean(slots);
            trackerBean.getSlots()
                    .addListAddListener(changeSlots -> { changeSlots.forEach(this::insertTracker); })
                    .addListUpdateListener(this::insertTracker)
                    .addListRemoveListener(changeSlots -> { changeSlots.forEach(this::removeTracker); });

            trackerBean.start();
            LOGGER.info("Tracker started");
        }

        return trackerBean;
    }

    protected void insertTracker(TrackerBeanSlot slot) {
        try {
            if (!getBean().getSlots().contains(slot))
                return;
        } catch (SQLException throwables) {
            LOGGER.error("Cound not load tracker bean", throwables);
        }

        DBMain.getInstance().asyncUpdate("REPLACE INTO Tracking (serverId, channelId, command, messageId, commandKey, time, arg) VALUES (?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, slot.getServerId());
            preparedStatement.setLong(2, slot.getChannelId());
            preparedStatement.setString(3, slot.getCommandTrigger());

            Optional<Long> messageIdOpt = slot.getMessageId();
            if (messageIdOpt.isPresent()) preparedStatement.setLong(4, messageIdOpt.get());
            else preparedStatement.setNull(4, Types.BIGINT);

            preparedStatement.setString(5, slot.getCommandKey());
            preparedStatement.setString(6, DBMain.instantToDateTimeString(slot.getNextRequest()));

            Optional<String> argsOpt = slot.getArgs();
            if (argsOpt.isPresent()) preparedStatement.setString(7, argsOpt.get());
            else preparedStatement.setNull(7, Types.VARCHAR);
        });
    }

    protected void removeTracker(TrackerBeanSlot slot) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Tracking WHERE channelId = ? AND command = ? AND commandKey = ?;", preparedStatement -> {
            preparedStatement.setLong(1, slot.getChannelId());
            preparedStatement.setString(2, slot.getCommandTrigger());
            preparedStatement.setString(3, slot.getCommandKey());
        });
    }

    @Override
    public void clear() {
        if (trackerBean != null) {
            trackerBean.stop();
            trackerBean = null;
            start();
        }
    }

}
