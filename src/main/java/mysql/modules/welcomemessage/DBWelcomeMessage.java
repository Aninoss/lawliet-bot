package mysql.modules.welcomemessage;

import java.util.Locale;
import commands.Category;
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
                "SELECT activated, title, description, channel, embed, goodbye, goodbyeText, goodbyeChannel, goodbyeEmbed, dm, dmText, dmEmbed, banner FROM ServerWelcomeMessage WHERE serverId = ?;",
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
                                resultSet.getBoolean(6),
                                resultSet.getString(7),
                                resultSet.getLong(8),
                                resultSet.getBoolean(9),
                                resultSet.getBoolean(10),
                                resultSet.getString(11),
                                resultSet.getBoolean(12),
                                resultSet.getBoolean(13)
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
                                true,
                                false,
                                TextManager.getString(locale, Category.UTILITY, "welcome_standard_goodbye"),
                                0L,
                                true,
                                false,
                                "",
                                true,
                                true
                        );
                    }
                }
        );
    }

    @Override
    protected void save(WelcomeMessageData welcomeMessageBean) {
        MySQLManager.asyncUpdate("REPLACE INTO ServerWelcomeMessage (serverId, activated, title, description, channel, embed, goodbye, goodbyeText, goodbyeChannel, goodbyeEmbed, dm, dmText, dmEmbed, banner) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, welcomeMessageBean.getGuildId());
            preparedStatement.setBoolean(2, welcomeMessageBean.isWelcomeActive());
            preparedStatement.setString(3, welcomeMessageBean.getWelcomeTitle());
            preparedStatement.setString(4, welcomeMessageBean.getWelcomeText());
            preparedStatement.setLong(5, welcomeMessageBean.getWelcomeChannelId());
            preparedStatement.setBoolean(6, welcomeMessageBean.getWelcomeEmbed());
            preparedStatement.setBoolean(7, welcomeMessageBean.isGoodbyeActive());
            preparedStatement.setString(8, welcomeMessageBean.getGoodbyeText());
            preparedStatement.setLong(9, welcomeMessageBean.getGoodbyeChannelId());
            preparedStatement.setBoolean(10, welcomeMessageBean.getGoodbyeEmbed());
            preparedStatement.setBoolean(11, welcomeMessageBean.isDmActive());
            preparedStatement.setString(12, welcomeMessageBean.getDmText());
            preparedStatement.setBoolean(13, welcomeMessageBean.getDmEmbed());
            preparedStatement.setBoolean(14, welcomeMessageBean.getBanner());
        });
    }

}
