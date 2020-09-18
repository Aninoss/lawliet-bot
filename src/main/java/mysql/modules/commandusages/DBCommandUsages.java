package mysql.modules.commandusages;

import mysql.DBBeanGenerator;
import mysql.DBMain;
import mysql.interfaces.IntervalSave;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBCommandUsages extends DBBeanGenerator<String, CommandUsagesBean> implements IntervalSave {

    private static final DBCommandUsages ourInstance = new DBCommandUsages();
    public static DBCommandUsages getInstance() { return ourInstance; }
    private DBCommandUsages() {}

    @Override
    protected CommandUsagesBean loadBean(String command) throws Exception {
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
    protected void saveBean(CommandUsagesBean commandUsagesBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO CommandUsages (command, usages) VALUES (?, ?);", preparedStatement -> {
            preparedStatement.setString(1, commandUsagesBean.getCommand());
            preparedStatement.setLong(2, commandUsagesBean.getValue());
        });
    }

    @Override
    public int getIntervalMinutes() { return 20; }

}