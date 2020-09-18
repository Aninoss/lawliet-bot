package mysql.modules.welcomemessage;

import constants.Category;
import core.TextManager;
import mysql.DBBeanGenerator;
import mysql.DBMain;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

public class DBWelcomeMessage extends DBBeanGenerator<Long, WelcomeMessageBean> {

    private static final DBWelcomeMessage ourInstance = new DBWelcomeMessage();
    public static DBWelcomeMessage getInstance() { return ourInstance; }
    private DBWelcomeMessage() {}

    @Override
    protected WelcomeMessageBean loadBean(Long serverId) throws Exception {
        WelcomeMessageBean welcomeMessageBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT activated, title, description, channel, goodbye, goodbyeText, goodbyeChannel, dm, dmText FROM ServerWelcomeMessage WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            welcomeMessageBean = new WelcomeMessageBean(
                    DBServer.getInstance().getBean(serverId),
                    resultSet.getBoolean(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getLong(4),
                    resultSet.getBoolean(5),
                    resultSet.getString(6),
                    resultSet.getLong(7),
                    resultSet.getBoolean(8),
                    resultSet.getString(9)
            );
        } else {
            ServerBean serverBean = DBServer.getInstance().getBean(serverId);
            Locale locale = serverBean.getLocale();

            welcomeMessageBean = new WelcomeMessageBean(
                    serverBean,
                    false,
                    TextManager.getString(locale, Category.MANAGEMENT, "welcome_standard_title"),
                    TextManager.getString(locale, Category.MANAGEMENT, "welcome_standard_description"),
                    0L,
                    false,
                    TextManager.getString(locale, Category.MANAGEMENT, "welcome_standard_goodbye"),
                    0L,
                    false,
                    ""
            );
        }

        resultSet.close();
        preparedStatement.close();

        return welcomeMessageBean;
    }

    @Override
    protected void saveBean(WelcomeMessageBean welcomeMessageBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO ServerWelcomeMessage (serverId, activated, title, description, channel, goodbye, goodbyeText, goodbyeChannel, dm, dmText) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, welcomeMessageBean.getServerId());

            preparedStatement.setBoolean(2, welcomeMessageBean.isWelcomeActive());
            preparedStatement.setString(3, welcomeMessageBean.getWelcomeTitle());
            preparedStatement.setString(4, welcomeMessageBean.getWelcomeText());
            preparedStatement.setLong(5, welcomeMessageBean.getWelcomeChannelId());
            preparedStatement.setBoolean(6, welcomeMessageBean.isGoodbyeActive());
            preparedStatement.setString(7, welcomeMessageBean.getGoodbyeText());
            preparedStatement.setLong(8, welcomeMessageBean.getGoodbyeChannelId());
            preparedStatement.setBoolean(9, welcomeMessageBean.isDmActive());
            preparedStatement.setString(10, welcomeMessageBean.getDmText());
        });
    }

}
