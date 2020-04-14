package MySQL.Modules.CommandUsages;

import MySQL.DBBeanGenerator;
import MySQL.DBMain;
import MySQL.Interfaces.IntervalSave;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBCommandUsages extends DBBeanGenerator<String, CommandUsagesBean> implements IntervalSave {

    private static final DBCommandUsages ourInstance = new DBCommandUsages();
    public static DBCommandUsages getInstance() { return ourInstance; }
    private DBCommandUsages() {}

    @Override
    protected CommandUsagesBean loadBean(String command) throws Exception {
        return new CommandUsagesBean(command);
    }

    @Override
    protected void saveBean(CommandUsagesBean commandUsagesBean) {
        DBMain.getInstance().asyncUpdate("INSERT INTO CommandUsages (command, usages) VALUES (?, ?) ON DUPLICATE KEY UPDATE usages = usages + ?;", preparedStatement -> {
            preparedStatement.setString(1, commandUsagesBean.getCommand());
            preparedStatement.setLong(2, commandUsagesBean.getAdd());
            preparedStatement.setLong(3, commandUsagesBean.getAdd());
            commandUsagesBean.reset();
        });
    }

    @Override
    public int getIntervalMinutes() { return 20; }

}