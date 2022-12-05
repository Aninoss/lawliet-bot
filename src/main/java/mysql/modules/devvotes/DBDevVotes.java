package mysql.modules.devvotes;

import java.util.Map;
import mysql.DBDataLoad;
import mysql.DBSingleCache;

public class DBDevVotes extends DBSingleCache<Map<Long, DevVotesSlot>> {

    private static final DBDevVotes ourInstance = new DBDevVotes();

    public static DBDevVotes getInstance() {
        return ourInstance;
    }

    private DBDevVotes() {
    }

    @Override
    protected Map<Long, DevVotesSlot> loadBean() {
        return new DBDataLoad<DevVotesSlot>("DevVotesReminders", "userId, active, locale", "1")
                .getMap(
                        DevVotesSlot::getUserId,
                        resultSet -> {
                            boolean active = resultSet.getBoolean(2);
                            if (resultSet.wasNull()) {
                                active = true;
                            }
                            return new DevVotesSlot(
                                    resultSet.getLong(1),
                                    active,
                                    resultSet.getString(3)
                            );
                        }
                );
    }

}
