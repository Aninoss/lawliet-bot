package MySQL.Modules.WelcomeMessage;

import Core.TextManager;
import MySQL.DBBeanGenerator;
import MySQL.DBMain;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.DiscordEntity;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Optional;

public class DBWelcomeMessage extends DBBeanGenerator<Long, WelcomeMessageBean> {

    private static final DBWelcomeMessage ourInstance = new DBWelcomeMessage();
    public static DBWelcomeMessage getInstance() { return ourInstance; }
    private DBWelcomeMessage() {}

    @Override
    protected WelcomeMessageBean loadBean(Long serverId) throws Exception {
        WelcomeMessageBean welcomeMessageBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT activated, title, description, channel, goodbye, goodbyeText, goodbyeChannel FROM ServerWelcomeMessage WHERE serverId = ?;");
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
                    resultSet.getLong(7)
            );
        } else {
            ServerBean serverBean = DBServer.getInstance().getBean(serverId);
            Locale locale = serverBean.getLocale();

            welcomeMessageBean = new WelcomeMessageBean(
                    serverBean,
                    false,
                    TextManager.getString(locale, TextManager.COMMANDS, "welcome_standard_title"),
                    TextManager.getString(locale, TextManager.COMMANDS, "welcome_standard_description"),
                    0L,
                    false,
                    TextManager.getString(locale, TextManager.COMMANDS, "welcome_standard_goodbye"),
                    0L
            );
        }

        resultSet.close();
        preparedStatement.close();

        return welcomeMessageBean;
    }

    @Override
    protected void saveBean(WelcomeMessageBean welcomeMessageBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO ServerWelcomeMessage (serverId, activated, title, description, channel, goodbye, goodbyeText, goodbyeChannel) VALUES (?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, welcomeMessageBean.getServerId());

            preparedStatement.setBoolean(2, welcomeMessageBean.isWelcomeActive());
            preparedStatement.setString(3, welcomeMessageBean.getWelcomeTitle());
            preparedStatement.setString(4, welcomeMessageBean.getWelcomeText());
            preparedStatement.setLong(5, welcomeMessageBean.getWelcomeChannelId());
            preparedStatement.setBoolean(6, welcomeMessageBean.isGoodbyeActive());
            preparedStatement.setString(7, welcomeMessageBean.getGoodbyeText());
            preparedStatement.setLong(8, welcomeMessageBean.getGoodbyeChannelId());
        });
    }

}
