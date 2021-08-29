package mysql.modules.welcomemessage;

import java.util.Locale;
import constants.Category;
import core.TextManager;
import mysql.MySQLManager;
import mysql.DBObserverMapCache;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;

public class DBWelcomeMessage extends DBObserverMapCache<Long, WelcomeMessageData> {

    private static final DBWelcomeMessage ourInstance = new DBWelcomeMessage();

    public static DBWelcomeMessage getInstance() {
        return ourInstance;
    }

    private DBWelcomeMessage() {
    }

    @Override
    protected WelcomeMessageData load(Long serverId) throws Exception {
        return MySQLManager.get(
                "SELECT activated, title, description, channel, goodbye, goodbyeText, goodbyeChannel, dm, dmText FROM ServerWelcomeMessage WHERE serverId = ?;",
                preparedStatement -> preparedStatement.setLong(1, serverId),
                resultSet -> {
                    if (resultSet.next()) {
                        return new WelcomeMessageData(
                                serverId,
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
                        GuildData guildBean = DBGuild.getInstance().retrieve(serverId);
                        Locale locale = guildBean.getLocale();

                        return new WelcomeMessageData(
                                serverId,
                                false,
                                TextManager.getString(locale, Category.UTILITY, "welcome_standard_title"),
                                TextManager.getString(locale, Category.UTILITY, "welcome_standard_description"),
                                0L,
                                false,
                                TextManager.getString(locale, Category.UTILITY, "welcome_standard_goodbye"),
                                0L,
                                false,
                                ""
                        );
                    }
                }
        );
    }

    @Override
    protected void save(WelcomeMessageData welcomeMessageBean) {
        MySQLManager.asyncUpdate("REPLACE INTO ServerWelcomeMessage (serverId, activated, title, description, channel, goodbye, goodbyeText, goodbyeChannel, dm, dmText) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, welcomeMessageBean.getGuildId());

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
