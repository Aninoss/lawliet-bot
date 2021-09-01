package mysql.modules.version;

import java.util.List;
import mysql.DBDataLoad;
import mysql.DBSingleCache;
import mysql.MySQLManager;

public class DBVersion extends DBSingleCache<VersionData> {

    private static final DBVersion ourInstance = new DBVersion();

    public static DBVersion getInstance() {
        return ourInstance;
    }

    private DBVersion() {
    }

    @Override
    protected VersionData loadBean() {
        List<VersionSlot> slots = new DBDataLoad<VersionSlot>("Version", "version, date", "1 ORDER BY date")
                .getList(
                        resultSet -> new VersionSlot(
                                resultSet.getString(1),
                                resultSet.getTimestamp(2).toInstant()
                        )
                );

        VersionData versionData = new VersionData(slots);
        versionData.getSlots().addListAddListener(list -> list.forEach(this::insertVersion));

        return versionData;
    }

    protected void insertVersion(VersionSlot versionSlot) {
        MySQLManager.asyncUpdate("INSERT IGNORE INTO Version (version, date) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setString(1, versionSlot.getVersion());
            preparedStatement.setString(2, MySQLManager.instantToDateTimeString(versionSlot.getDate()));
        });
    }

}
