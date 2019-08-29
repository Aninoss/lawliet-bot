package MySQL;

import General.Survey.*;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class DBSurvey {
    public static Survey getCurrentSurvey() throws Throwable {
        Statement statement = DBMain.getInstance().statement("SELECT * FROM SurveyDates ORDER BY surveyId DESC LIMIT 1;");
        ResultSet resultSet = statement.getResultSet();
        Survey survey = null;
        if (resultSet.next()) {
            survey = new Survey(
                    resultSet.getInt(1),
                    resultSet.getTimestamp(2).toInstant()
            );
        }
        resultSet.close();
        statement.close();
        return survey;
    }

    public static UserVoteData getUserVotes(User user) throws Throwable {
        String sql = "SELECT IFNULL(personalVote, -1) FROM SurveyVotes WHERE surveyId = (SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1) AND userId = ?;" +
                "SELECT serverId, majorityVote FROM SurveyMajorityVotes WHERE surveyId = (SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1) AND userId = ?;";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, user.getId());
        preparedStatement.setLong(2, user.getId());

        int i=0;
        UserVoteData userVoteData = new UserVoteData();
        userVoteData.setUser(user);
        ArrayList<UserMajorityVoteData> userMajorityVoteData = new ArrayList<>();
        for(ResultSet resultSet: new DBMultipleResultSet(preparedStatement)) {
            if (i == 0) {
                if (resultSet.next()) {
                    userVoteData.setPersonalVote(resultSet.getInt(1));
                }
            } else if (i == 1) {
                while (resultSet.next()) {
                    Server server = user.getApi().getServerById(resultSet.getLong(1)).get();
                    userMajorityVoteData.add(new UserMajorityVoteData(server, resultSet.getInt(2)));
                }
                userVoteData.setMajorityVotes(userMajorityVoteData);
            }

            i++;
            resultSet.close();
        }
        preparedStatement.close();

        return userVoteData;
    }

    public static ArrayList<SurveyServer> getUsersWithRightChoiceForCurrentSurvey(DiscordApi api) throws Throwable {
        String sql = "SELECT serverId, userId, (majorityVote = (SELECT personalVote FROM SurveyVotes WHERE surveyId = (SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1) GROUP BY personalVote ORDER BY COUNT(personalVote) DESC LIMIT 1))\n" +
                "FROM SurveyMajorityVotes \n" +
                "WHERE (SELECT powerPlant FROM DServer WHERE DServer.serverId = SurveyMajorityVotes.serverId) = 'ACTIVE'\n" +
                "AND surveyId = (SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1)\n" +
                "ORDER BY serverId;";

        Statement statement = DBMain.getInstance().statement(sql);

        ResultSet resultSet = statement.getResultSet();
        ArrayList<SurveyUser> userList = null;
        ArrayList<SurveyServer> serverList = new ArrayList<>();
        long lastServerId = -1;
        while (resultSet.next()) {
            long serverId = resultSet.getLong(1);
            if (api.getServerById(serverId).isPresent()) {
                if (serverId != lastServerId) {
                    userList = new ArrayList<>();
                    serverList.add(new SurveyServer(api.getServerById(serverId).get(), userList));
                }
                try {
                    userList.add(new SurveyUser(api.getUserById(resultSet.getLong(2)).get(), resultSet.getBoolean(3)));
                } catch (InterruptedException | ExecutionException | SQLException e) {
                    //Ignore
                }
            }
            lastServerId = serverId;
        }
        resultSet.close();
        statement.close();

        return serverList;
    }

    public static void updatePersonalVote(User user, int personalVote) throws Throwable {
        String sql = "INSERT INTO SurveyVotes (surveyId, userId, personalVote) VALUES ((SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1), ?, ?) "
                + "ON DUPLICATE KEY UPDATE personalVote = ?;";

        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement(sql);
        baseStatement.setLong(1, user.getId());
        baseStatement.setInt(2, personalVote);
        baseStatement.setInt(3, personalVote);
        baseStatement.executeUpdate();
        baseStatement.close();
    }

    public static boolean updateMajorityVote(Server server, User user, int majorityVote) throws Throwable {
        String sql = "SELECT COUNT(*) FROM SurveyVotes WHERE surveyId = (SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1) AND userId = ?";

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement(sql);
        preparedStatement.setLong(1, user.getId());
        preparedStatement.execute();
        ResultSet resultSet = preparedStatement.getResultSet();
        boolean ok = false;

        if (resultSet.next() && resultSet.getInt(1) > 0) ok = true;

        resultSet.close();
        preparedStatement.close();

        if (!ok) return false;

        sql = "INSERT INTO SurveyMajorityVotes (surveyId, serverId, userId, majorityVote) VALUES ((SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1), ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE majorityVote = ?;";

        PreparedStatement baseStatement = DBMain.getInstance().preparedStatement(sql);
        baseStatement.setLong(1, server.getId());
        baseStatement.setLong(2, user.getId());
        baseStatement.setInt(3, majorityVote);
        baseStatement.setInt(4, majorityVote);
        baseStatement.executeUpdate();
        baseStatement.close();

        return true;
    }

    public static void nextSurvey() throws Throwable {
        String sql = "INSERT INTO SurveyDates VALUES ((SELECT surveyId FROM SurveyDates a ORDER BY a.surveyId DESC LIMIT 1) + 1, NOW());";
        Statement statement = DBMain.getInstance().statement(sql);
        statement.close();
    }

    public static SurveyResults getResults() throws Throwable {
        String sql = "SELECT surveyId-1 FROM SurveyDates ORDER BY surveyId DESC LIMIT 1;" +
                "SELECT personalVote, COUNT(personalVote) FROM SurveyVotes WHERE surveyId = (SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1)-1 GROUP BY personalVote;" +
                "SELECT majorityVote, COUNT(majorityVote) FROM SurveyMajorityVotes WHERE surveyId = (SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1)-1 GROUP BY majorityVote;";

        int surveyId = -1;
        int[] userVote = new int[2];
        int[] majorityVote = new int[2];
        int i = 0;
        for(ResultSet resultSet: new DBMultipleResultSet(sql)) {
            while (resultSet.next()) {
                switch (i) {
                    case 0:
                        surveyId = resultSet.getInt(1);
                        break;

                    case 1:
                        userVote[resultSet.getInt(1)] = resultSet.getInt(2);
                        break;

                    case 2:
                        majorityVote[resultSet.getInt(1)] = resultSet.getInt(2);
                        break;
                }
            }
            i++;
        }

        return new SurveyResults(userVote, majorityVote, surveyId);
    }

    public static int getCurrentVotesNumber() throws Throwable {
        String sql = "SELECT COUNT(*) FROM SurveyVotes WHERE surveyId = (SELECT surveyId FROM SurveyDates ORDER BY surveyId DESC LIMIT 1);";

        Statement statement = DBMain.getInstance().statement(sql);
        ResultSet resultSet = statement.getResultSet();

        int number = -1;

        if (resultSet.next()) number = resultSet.getInt(1);

        statement.close();
        resultSet.close();

        return number;
    }
}
