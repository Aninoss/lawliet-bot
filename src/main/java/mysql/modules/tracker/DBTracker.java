package mysql.modules.tracker;

import core.CustomThread;
import core.DiscordApiManager;
import core.TrackerManager;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleBeanGenerator;
import mysql.modules.server.DBServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class DBTracker extends DBSingleBeanGenerator<TrackerBean> {

    private final static Logger LOGGER = LoggerFactory.getLogger(DBTracker.class);

    private static final DBTracker ourInstance = new DBTracker();
    public static DBTracker getInstance() { return ourInstance; }
    private DBTracker() {}

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        new CustomThread(() -> {
            TrackerManager.getInstance().start(getBean());
            LOGGER.info("Tracker started");
        }, "tracker_init").start();
    }

    @Override
    protected TrackerBean loadBean() throws Exception {
        ArrayList<TrackerBeanSlot> slots = new DBDataLoad<TrackerBeanSlot>("Tracking", "serverId, channelId, command, messageId, commandKey, time, arg", "1", preparedStatement -> {})
                .getArrayList(
                        resultSet -> new TrackerBeanSlot(
                                DBServer.getInstance().getBean(resultSet.getLong(1)),
                                resultSet.getLong(2),
                                resultSet.getString(3),
                                resultSet.getLong(4),
                                resultSet.getString(5),
                                resultSet.getTimestamp(6).toInstant(),
                                resultSet.getString(7)
                        )
                );
        slots.removeIf(o -> Objects.isNull(o) || !DiscordApiManager.getInstance().serverIsManaged(o.getServerId()));

        TrackerBean trackerBean = new TrackerBean(slots);
        trackerBean.getSlots()
                .addListAddListener(changeSlots -> { changeSlots.forEach(this::insertTracker); })
                .addListUpdateListener(this::insertTracker)
                .addListRemoveListener(changeSlots -> { changeSlots.forEach(this::removeTracker); });

        return trackerBean;
    }

    protected void insertTracker(TrackerBeanSlot slot) {
        if (!Objects.isNull(slot)) {
            if (!getBean().getSlots().contains(slot))
                return;

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
    }

    protected void removeTracker(TrackerBeanSlot slot) {
        if (!Objects.isNull(slot)) {
            DBMain.getInstance().asyncUpdate("DELETE FROM Tracking WHERE channelId = ? AND command = ? AND commandKey = ?;", preparedStatement -> {
                preparedStatement.setLong(1, slot.getChannelId());
                preparedStatement.setString(2, slot.getCommandTrigger());
                preparedStatement.setString(3, slot.getCommandKey());
            });
        }
    }

    @Override
    public void clear() {
        if (isCached()) {
            super.clear();
            start();
        }
    }

}
