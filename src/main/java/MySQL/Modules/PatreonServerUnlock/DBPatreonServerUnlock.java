package MySQL.Modules.PatreonServerUnlock;

import MySQL.DBBeanGenerator;
import MySQL.DBDataLoad;
import MySQL.DBMain;
import MySQL.Modules.Server.DBServer;

import java.sql.SQLException;
import java.util.ArrayList;

public class DBPatreonServerUnlock extends DBBeanGenerator<Long, PatreonServerUnlockBean> {

    private static final DBPatreonServerUnlock ourInstance = new DBPatreonServerUnlock();
    public static DBPatreonServerUnlock getInstance() { return ourInstance; }
    private DBPatreonServerUnlock() {}

    @Override
    protected PatreonServerUnlockBean loadBean(Long serverId) throws Exception {
        PatreonServerUnlockBean patreonServerUnlockBean = new PatreonServerUnlockBean(
                DBServer.getInstance().getBean(serverId),
                getUserIds(serverId)
        );

        patreonServerUnlockBean.getUserSlots()
                .addListAddListener(list -> list.forEach(userSlot -> addUserId(patreonServerUnlockBean.getServerId(), userSlot)))
                .addListUpdateListener(userSlot -> addUserId(patreonServerUnlockBean.getServerId(), userSlot))
                .addListRemoveListener(list -> list.forEach(userSlot -> removeUserId(patreonServerUnlockBean.getServerId(), userSlot)));

        return patreonServerUnlockBean;
    }

    public void removeUser(long userId) {
        getCache().asMap().values().forEach(bean -> bean.getUserSlots().removeIf(slot -> slot.getUserId() == userId));
        DBMain.getInstance().asyncUpdate("DELETE FROM PatreonServerUnlock WHERE userId = ?;", preparedStatement -> preparedStatement.setLong(1, userId));
    }

    @Override
    protected void saveBean(PatreonServerUnlockBean patreonServerUnlockBean) {}

    private ArrayList<PatreonUserSlot> getUserIds(long serverId) throws SQLException {
        return new DBDataLoad<PatreonUserSlot>("PatreonServerUnlock", "userId, dateTime", "serverId = ?",
                preparedStatement -> preparedStatement.setLong(1, serverId)
        ).getArrayList(resultSet -> new PatreonUserSlot(resultSet.getLong(1), resultSet.getTimestamp(2).toInstant()));
    }

    private void addUserId(long serverId, PatreonUserSlot patreonUserSlot) {
        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO PatreonServerUnlock (serverId, userId, dateTime) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, patreonUserSlot.getUserId());
            preparedStatement.setString(3, DBMain.instantToDateTimeString(patreonUserSlot.getUnlockTime()));
        });
    }

    private void removeUserId(long serverId, PatreonUserSlot patreonUserSlot) {
        DBMain.getInstance().asyncUpdate("DELETE FROM PatreonServerUnlock WHERE serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, serverId);
            preparedStatement.setLong(2, patreonUserSlot.getUserId());
        });
    }

}
