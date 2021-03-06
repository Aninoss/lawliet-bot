package mysql.modules.commandusages;

import mysql.DBIntervalMapCache;
import mysql.DBMain;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBCommandUsages extends DBIntervalMapCache<String, CommandUsagesBean> {

    private static final DBCommandUsages ourInstance = new DBCommandUsages();

    public static DBCommandUsages getInstance() {
        return ourInstance;
    }

    private DBCommandUsages() {
        super(20);
    }

    @Override
    protected CommandUsagesBean load(String command) throws Exception {
        CommandUsagesBean commandUsagesBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT usages FROM CommandUsages WHERE command = ?;");
        preparedStatement.setString(1, command);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            commandUsagesBean = new CommandUsagesBean(
                    command,
                    resultSet.getLong(1)
            );
        } else {
            commandUsagesBean = new CommandUsagesBean(
                    command,
                    0L
            );
        }

        resultSet.close();
        preparedStatement.close();

        return commandUsagesBean;
    }

    @Override
    protected void save(CommandUsagesBean commandUsagesBean) {
        DBMain.getInstance().asyncUpdate("INSERT INTO CommandUsages (command, usages) VALUES (?, ?) ON DUPLICATE KEY UPDATE usages = usages + ?;", preparedStatement -> {
            long inc = commandUsagesBean.flushIncrement();
            preparedStatement.setString(1, commandUsagesBean.getCommand());
            preparedStatement.setLong(2, inc);
            preparedStatement.setLong(3, inc);
        });
    }

}