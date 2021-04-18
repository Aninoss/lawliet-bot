package mysql.modules.commandusages;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import mysql.DBIntervalMapCache;
import mysql.DBMain;

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
        CommandUsagesData commandUsagesData;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT usages FROM CommandUsages WHERE command = ?;");
        preparedStatement.setString(1, command);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            commandUsagesData = new CommandUsagesData(
                    command,
                    resultSet.getLong(1)
            );
        } else {
            commandUsagesData = new CommandUsagesData(
                    command,
                    0L
            );
        }

        resultSet.close();
        preparedStatement.close();

        return commandUsagesData;
    }

    @Override
    protected void save(CommandUsagesData commandUsagesData) {
        DBMain.getInstance().asyncUpdate("INSERT INTO CommandUsages (command, usages) VALUES (?, ?) ON DUPLICATE KEY UPDATE usages = usages + ?;", preparedStatement -> {
            long inc = commandUsagesData.flushIncrement();
            preparedStatement.setString(1, commandUsagesData.getCommand());
            preparedStatement.setLong(2, inc);
            preparedStatement.setLong(3, inc);
        });
    }

}