package mysql.modules.warning;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import javafx.util.Pair;
import mysql.DBDataLoad;
import mysql.DBObserverMapCache;
import mysql.MySQLManager;

public class DBServerWarnings extends DBObserverMapCache<Pair<Long, Long>, ServerWarningsData> {

    private static final DBServerWarnings ourInstance = new DBServerWarnings();

    public static DBServerWarnings getInstance() {
        return ourInstance;
    }

    private DBServerWarnings() {
    }

    @Override
    protected ServerWarningsData load(Pair<Long, Long> pair) throws Exception {
        ServerWarningsData serverWarningsBean = new ServerWarningsData(
                pair.getKey(),
                pair.getValue(),
                getWarnings(pair.getKey(), pair.getValue())
        );

        serverWarningsBean.getWarnings()
                .addListAddListener(list -> list.forEach(this::addWarning))
                .addListRemoveListener(list -> list.forEach(this::removeWarning));

        return serverWarningsBean;
    }

    @Override
    protected void save(ServerWarningsData serverWarningsBean) {
    }

    private List<ServerWarningSlot> getWarnings(long serverId, long userId) {
        return new DBDataLoad<ServerWarningSlot>("Warnings", "userId, time, requestorUserId, reason", "serverId = ? AND userId = ? ORDER BY time",
                preparedStatement -> {
                    preparedStatement.setLong(1, serverId);
                    preparedStatement.setLong(2, userId);
                }
        ).getList(resultSet -> new ServerWarningSlot(
                serverId,
                resultSet.getLong(1),
                resultSet.getTimestamp(2).toInstant(),
                resultSet.getLong(3),
                resultSet.getString(4)
        ));
    }

    private void addWarning(ServerWarningSlot serverWarningsSlot) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO Warnings (serverId, userId, time, requestorUserId, reason) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverWarningsSlot.getGuildId());
            preparedStatement.setLong(2, serverWarningsSlot.getMemberId());
            preparedStatement.setString(3, MySQLManager.instantToDateTimeString(serverWarningsSlot.getTime()));
            preparedStatement.setLong(4, serverWarningsSlot.getRequesterUserId());

            Optional<String> reason = serverWarningsSlot.getReason();
            if (reason.isPresent()) {
                preparedStatement.setString(5, reason.get());
            } else {
                preparedStatement.setNull(5, Types.VARCHAR);
            }
        });
    }

    private void removeWarning(ServerWarningSlot serverWarningsSlot) {
        MySQLManager.asyncUpdate("DELETE FROM Warnings WHERE serverId = ? AND userId = ? AND time = ? AND requestorUserId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverWarningsSlot.getGuildId());
            preparedStatement.setLong(2, serverWarningsSlot.getMemberId());
            preparedStatement.setString(3, MySQLManager.instantToDateTimeString(serverWarningsSlot.getTime()));
            preparedStatement.setLong(4, serverWarningsSlot.getRequesterUserId());
        });
    }

}