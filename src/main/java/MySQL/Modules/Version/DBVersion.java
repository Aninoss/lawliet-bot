package MySQL.Modules.Version;

import MySQL.DBCached;
import MySQL.DBDataLoad;
import MySQL.DBMain;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBVersion extends DBCached {

    private static DBVersion ourInstance = new DBVersion();
    public static DBVersion getInstance() { return ourInstance; }
    private DBVersion() {}

    private VersionBean versionBean;

    public synchronized VersionBean getBean() throws SQLException {
        if (versionBean == null) {
            ArrayList<VersionBeanSlot> slots = new DBDataLoad<VersionBeanSlot>("Version", "version, date", "1 ORDER BY date", preparedStatement -> {})
                    .getArrayList(
                            resultSet -> new VersionBeanSlot(
                                    resultSet.getString(1),
                                    resultSet.getTimestamp(2).toInstant()
                            )
                    );

            versionBean = new VersionBean(slots);
            versionBean.getSlots().addListAddListener(list -> list.forEach(this::insertVersion));
        }

        return versionBean;
    }

    protected void insertVersion(VersionBeanSlot versionBeanSlot) {
        DBMain.getInstance().asyncUpdate("INSERT INTO Version (version, date) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setString(1, versionBeanSlot.getVersion());
            preparedStatement.setString(2, DBMain.instantToDateTimeString(versionBeanSlot.getDate()));
        });
    }

    @Override
    public void clear() { versionBean = null; }

}
