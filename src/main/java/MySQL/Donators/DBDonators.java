package MySQL.Donators;

import MySQL.AutoQuote.AutoQuoteBean;
import MySQL.DBBeanGenerator;
import MySQL.DBKeySetLoad;
import MySQL.DBMain;
import MySQL.Server.DBServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

public class DBDonators extends DBBeanGenerator<Long, DonatorBean> implements DBBeanGenerator.CompleteLoadOnStartup<Long> {

    private static DBDonators ourInstance = new DBDonators();
    public static DBDonators getInstance() { return ourInstance; }
    private DBDonators() {}

    @Override
    protected DonatorBean loadBean(Long userId) throws Exception {
        DonatorBean donatorBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT end FROM Donators WHERE userId = ?;");
        preparedStatement.setLong(1, userId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            donatorBean = new DonatorBean(
                    userId,
                    resultSet.getDate(1).toLocalDate()
            );
        } else {
            donatorBean = new DonatorBean(
                    userId,
                    LocalDate.now()
            );
        }

        resultSet.close();
        preparedStatement.close();

        return donatorBean;
    }

    @Override
    protected void saveBean(DonatorBean donatorBean) throws SQLException {
        if (donatorBean.getDonationEnd().isAfter(LocalDate.now())) {
            PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("REPLACE INTO Donators (userId, end) VALUES (?, ?);");
            preparedStatement.setLong(1, donatorBean.getUserId());
            preparedStatement.setString(2, DBMain.localDateToDateString(donatorBean.getDonationEnd()));

            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
    }

    public void removeBean(DonatorBean donatorBean) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("DELETE FROM Donators WHERE userId = ?;");
        preparedStatement.setLong(1, donatorBean.getUserId());
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }

    @Override
    public ArrayList<Long> getKeySet() throws SQLException {
        return new DBKeySetLoad<Long>("Donators", "userId")
                .get(resultSet -> resultSet.getLong(1));
    }

}
