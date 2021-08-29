package mysql.modules.commandusages;

import mysql.DBIntervalMapCache;
import mysql.MySQLManager;

public class DBCommandUsages extends DBIntervalMapCache<String, CommandUsagesData> {

    private static final DBCommandUsages ourInstance = new DBCommandUsages();

    public static DBCommandUsages getInstance() {
        return ourInstance;
    }

    private DBCommandUsages() {
        super(20);
    }

    @Override
    protected CommandUsagesData load(String command) throws Exception {
        return MySQLManager.get(
                "SELECT usages FROM CommandUsages WHERE command = ?;",
                preparedStatement -> preparedStatement.setString(1, command),
                resultSet -> {
                    if (resultSet.next()) {
                        return new CommandUsagesData(
                                command,
                                resultSet.getLong(1)
                        );
                    } else {
                        return new CommandUsagesData(
                                command,
                                0L
                        );
                    }
                }
        );
    }

    @Override
    protected void save(CommandUsagesData commandUsagesData) {
        MySQLManager.asyncUpdate("INSERT INTO CommandUsages (command, usages) VALUES (?, ?) ON DUPLICATE KEY UPDATE usages = usages + ?;", preparedStatement -> {
            long inc = commandUsagesData.flushIncrement();
            preparedStatement.setString(1, commandUsagesData.getCommand());
            preparedStatement.setLong(2, inc);
            preparedStatement.setLong(3, inc);
        });
    }

}