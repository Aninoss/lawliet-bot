package MySQL.Modules.Donators;

import MySQL.DBCached;
import MySQL.DBMain;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class DBDonators extends DBCached {

    private static DBDonators ourInstance = new DBDonators();
    public static DBDonators getInstance() { return ourInstance; }
    private DBDonators() {}

    private DonatorBean donatorBean = null;

    public DonatorBean getBean() throws SQLException {
        if (donatorBean == null) {
            HashMap<Long, DonatorBeanSlot> slots = new HashMap<>();

            Statement statement = DBMain.getInstance().statement("SELECT userId, end FROM Donators;");
            ResultSet resultSet = statement.getResultSet();
            while (resultSet.next()) {
                long userId = resultSet.getLong(1);

                DonatorBeanSlot donatorBeanSlot = new DonatorBeanSlot(
                        userId,
                        resultSet.getDate(2).toLocalDate()
                );

                slots.put(userId, donatorBeanSlot);
            }

            resultSet.close();
            statement.close();

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

    public void removeDonation(DonatorBeanSlot donatorBean) {
        DBMain.getInstance().asyncUpdate("DELETE FROM Donators WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, donatorBean.getUserId());
        });
    }

    @Override
    public void clear() {
        donatorBean = null;
    }

}
