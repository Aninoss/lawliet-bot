package mysql.modules.donators;

import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBSingleBeanGenerator;

import java.util.HashMap;

public class DBDonators extends DBSingleBeanGenerator<DonatorBean> {

    private static final DBDonators ourInstance = new DBDonators();
    public static DBDonators getInstance() { return ourInstance; }
    private DBDonators() {}

    @Override
    protected DonatorBean loadBean() throws Exception {
        HashMap<Long, DonatorBeanSlot> slots = new DBDataLoad<DonatorBeanSlot>("Donators", "userId, end, totalDollars", "1", preparedStatement -> {})
                .getHashMap(
                        DonatorBeanSlot::getUserId,
                        resultSet -> new DonatorBeanSlot(
                                resultSet.getLong(1),
                                resultSet.getDate(2).toLocalDate(),
                                resultSet.getDouble(3)
                        )
                );

        DonatorBean donatorBean = new DonatorBean(slots);
        donatorBean.getMap()
                .addMapAddListener(this::insertDonation)
                .addMapUpdateListener(this::insertDonation);

        return donatorBean;
    }

    protected void insertDonation(DonatorBeanSlot donatorBean) {
        if (donatorBean.getTotalDollars() > 0) {
            DBMain.getInstance().asyncUpdate("REPLACE INTO Donators (userId, end, totalDollars) VALUES (?, ?, ?);", preparedStatement -> {
                preparedStatement.setLong(1, donatorBean.getUserId());
                preparedStatement.setString(2, DBMain.localDateToDateString(donatorBean.getDonationEnd()));
                preparedStatement.setDouble(3, donatorBean.getTotalDollars());
            });
        }
    }

}
