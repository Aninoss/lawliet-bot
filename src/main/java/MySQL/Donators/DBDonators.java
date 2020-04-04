package MySQL.Donators;

import MySQL.DBBeanGenerator;
import MySQL.DBCached;
import MySQL.DBKeySetLoad;
import MySQL.DBMain;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
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
        try {
            if (donatorBean.isValid()) {
                PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO Donators (userId, end) VALUES (?, ?);");
                preparedStatement.setLong(1, donatorBean.getUserId());
                preparedStatement.setString(2, DBMain.localDateToDateString(donatorBean.getDonationEnd()));

                preparedStatement.executeUpdate();
                preparedStatement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeDonation(DonatorBeanSlot donatorBean) {
        try {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM Donators WHERE userId = ?;");
            preparedStatement.setLong(1, donatorBean.getUserId());
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void clear() {
        donatorBean = null;
    }

}
