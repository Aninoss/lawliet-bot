package MySQL.Modules.Donators;

import MySQL.DBCached;
import MySQL.DBDataLoad;
import MySQL.DBMain;
import java.sql.SQLException;
import java.util.HashMap;

public class DBDonators extends DBCached {

    private static final DBDonators ourInstance = new DBDonators();
    public static DBDonators getInstance() { return ourInstance; }
    private DBDonators() {}

    private DonatorBean donatorBean = null;

    public synchronized DonatorBean getBean() throws SQLException {
        if (donatorBean == null) {
            HashMap<Long, DonatorBeanSlot> slots = new DBDataLoad<DonatorBeanSlot>("Donators", "userId, end", "1", preparedStatement -> {})
                    .getHashMap(
                            DonatorBeanSlot::getUserId,
                            resultSet -> new DonatorBeanSlot(
                                    resultSet.getLong(1),
                                    resultSet.getDate(2).toLocalDate()
                            )
                    );

            donatorBean = new DonatorBean(slots);
            donatorBean.getMap()
                    .addMapAddListener(this::insertDonation)
                    .addMapUpdateListener(this::insertDonation)
                    .addMapRemoveListener(this::removeDonation);
        }

        return donatorBean;
    }

    protected void insertDonation(DonatorBeanSlot donatorBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO Donators (userId, end) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, donatorBean.getUserId());
            preparedStatement.setString(2, DBMain.localDateToDateString(donatorBean.getDonationEnd()));
        });
    }

    protected void removeDonation(DonatorBeanSlot donatorBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Donators WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, donatorBean.getUserId());
        });
    }

    @Override
    public void clear() {
        donatorBean = null;
    }

}
