package MySQL.CommandUsages;

import MySQL.DBBeanGenerator;
import MySQL.DBMain;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBCommandUsages extends DBBeanGenerator<String, CommandUsagesBean> implements DBBeanGenerator.IntervalSave {

    private static DBCommandUsages ourInstance = new DBCommandUsages();
    public static DBCommandUsages getInstance() { return ourInstance; }
    private DBCommandUsages() {}

    @Override
    protected CommandUsagesBean loadBean(String command) throws Exception {
        return new CommandUsagesBean(command);
    }

    @Override
    protected void saveBean(CommandUsagesBean commandUsagesBean) throws SQLException {
        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("INSERT INTO CommandUsages (command, usages) VALUES (?, ?) ON DUPLICATE KEY UPDATE usages = usages + ?;");
        preparedStatement.setString(1, commandUsagesBean.getCommand());
        preparedStatement.setLong(2, commandUsagesBean.getAdd());
        preparedStatement.setLong(3, commandUsagesBean.getAdd());

        preparedStatement.executeUpdate();
        preparedStatement.close();
        commandUsagesBean.reset();
    }

    @Override
    public int getIntervalMinutes() { return 20; }

}