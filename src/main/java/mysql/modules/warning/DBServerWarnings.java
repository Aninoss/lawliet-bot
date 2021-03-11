package mysql.modules.warning;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Optional;
import javafx.util.Pair;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBMapCache;

public class DBServerWarnings extends DBMapCache<Pair<Long, Long>, ServerWarningsBean> {

    private static final DBServerWarnings ourInstance = new DBServerWarnings();

    public static DBServerWarnings getInstance() {
        return ourInstance;
    }

    private DBServerWarnings() {
    }

    @Override
    protected ServerWarningsBean load(Pair<Long, Long> pair) throws Exception {
        ServerWarningsBean serverWarningsBean = new ServerWarningsBean(
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
    protected void save(ServerWarningsBean serverWarningsBean) {
    }

    private ArrayList<GuildWarningsSlot> getWarnings(long serverId, long userId) {
        return new DBDataLoad<GuildWarningsSlot>("Warnings", "userId, time, requestorUserId, reason", "serverId = ? AND userId = ? ORDER BY time",
                preparedStatement -> {
                    preparedStatement.setLong(1, serverId);
                    preparedStatement.setLong(2, userId);
                }
        ).getArrayList(resultSet -> new GuildWarningsSlot(
                serverId,
                resultSet.getLong(1),
                resultSet.getTimestamp(2).toInstant(),
                resultSet.getLong(3),
                resultSet.getString(4)
        ));
    }

    private void addWarning(GuildWarningsSlot serverWarningsSlot) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO Warnings (serverId, userId, time, requestorUserId, reason) VALUES (?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverWarningsSlot.getGuildId());
            preparedStatement.setLong(2, serverWarningsSlot.getMemberId());
            preparedStatement.setString(3, DBMain.instantToDateTimeString(serverWarningsSlot.getTime()));
            preparedStatement.setLong(4, serverWarningsSlot.getRequesterUserId());

            Optional<String> reason = serverWarningsSlot.getReason();
            if (reason.isPresent()) {
                preparedStatement.setString(5, reason.get());
            } else {
                preparedStatement.setNull(5, Types.VARCHAR);
            }
        });
    }

    private void removeWarning(GuildWarningsSlot serverWarningsSlot) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Warnings WHERE serverId = ? AND userId = ? AND time = ? AND requestorUserId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverWarningsSlot.getGuildId());
            preparedStatement.setLong(2, serverWarningsSlot.getMemberId());
            preparedStatement.setString(3, DBMain.instantToDateTimeString(serverWarningsSlot.getTime()));
            preparedStatement.setLong(4, serverWarningsSlot.getRequesterUserId());
        });
    }

}