package mysql.modules.moderation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Optional;
import mysql.DBMain;
import mysql.DBObserverMapCache;

public class DBModeration extends DBObserverMapCache<Long, ModerationBean> {

    private static final DBModeration ourInstance = new DBModeration();

    public static DBModeration getInstance() {
        return ourInstance;
    }

    private DBModeration() {
    }

    @Override
    protected ModerationBean load(Long serverId) throws Exception {
        ModerationBean moderationBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT channelId, question, autoKick, autoBan, autoKickDays, autoBanDays FROM Moderation WHERE serverId = ?;");
        preparedStatement.setLong(1, serverId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            moderationBean = new ModerationBean(
                    serverId,
                    resultSet.getLong(1),
                    resultSet.getBoolean(2),
                    resultSet.getInt(3),
                    resultSet.getInt(4),
                    resultSet.getInt(5),
                    resultSet.getInt(6)
            );
        } else {
            moderationBean = new ModerationBean(
                    serverId,
                    null,
                    true,
                    0,
                    0,
                    0,
                    0
            );
        }

        resultSet.close();
        preparedStatement.close();

        return moderationBean;
    }

    @Override
    protected void save(ModerationBean moderationBean) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO Moderation (serverId, channelId, question, autoKick, autoBan, autoKickDays, autoBanDays) VALUES (?, ?, ?, ?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setLong(1, moderationBean.getGuildId());

            Optional<Long> channelIdOpt = moderationBean.getAnnouncementChannelId();
            if (channelIdOpt.isPresent()) {
                preparedStatement.setLong(2, channelIdOpt.get());
            } else {
                preparedStatement.setNull(2, Types.BIGINT);
            }

            preparedStatement.setBoolean(3, moderationBean.isQuestion());
            preparedStatement.setInt(4, moderationBean.getAutoKick());
            preparedStatement.setInt(5, moderationBean.getAutoBan());
            preparedStatement.setInt(6, moderationBean.getAutoKickDays());
            preparedStatement.setInt(7, moderationBean.getAutoBanDays());
        });
    }

}
