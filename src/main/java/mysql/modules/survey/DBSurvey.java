package mysql.modules.survey;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import javafx.util.Pair;
import mysql.DBDataLoad;
import mysql.DBMain;
import mysql.DBObserverMapCache;

public class DBSurvey extends DBObserverMapCache<Integer, SurveyData> {

    private static final DBSurvey ourInstance = new DBSurvey();

    public static DBSurvey getInstance() {
        return ourInstance;
    }

    private DBSurvey() {
    }

    private Integer currentSurveyId = null;

    @Override
    protected SurveyData load(Integer surveyId) throws Exception {
        SurveyData surveyData;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT start FROM SurveyDates WHERE surveyId = ?;");
        preparedStatement.setInt(1, surveyId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            surveyData = new SurveyData(
                    surveyId,
                    resultSet.getDate(1).toLocalDate(),
                    getFirstVotes(surveyId),
                    getSecondVotes(surveyId),
                    getNotificationUserIds()
            );
        } else {
            surveyData = new SurveyData(
                    surveyId,
                    resultSet.getDate(1).toLocalDate(),
                    new HashMap<>(),
                    new HashMap<>(),
                    new ArrayList<>()
            );
        }

        resultSet.close();
        preparedStatement.close();

        surveyData.getFirstVotes()
                .addMapAddListener(firstVote -> addFirstVote(surveyId, firstVote))
                .addMapRemoveListener(firstVote -> removeFirstVote(surveyId, firstVote));
        surveyData.getSecondVotes()
                .addMapAddListener(secondVote -> addSecondVote(surveyId, secondVote))
                .addMapRemoveListener(secondVote -> removeSecondVote(surveyId, secondVote));
        surveyData.getNotificationUserIds()
                .addListAddListener(list -> list.forEach(this::addNotificationUserId))
                .addListRemoveListener(list -> list.forEach(this::removeNotificationUserId));

        return surveyData;
    }

    public SurveyData getCurrentSurvey() {
        return retrieve(getCurrentSurveyId());
    }

    public synchronized int getCurrentSurveyId() {
        if (currentSurveyId == null) {
            try {
                Statement statement = DBMain.getInstance().statementExecuted("SELECT surveyId FROM SurveyDates ORDER BY start DESC, surveyId DESC LIMIT 1;");
                ResultSet resultSet = statement.getResultSet();

                if (resultSet.next()) {
                    currentSurveyId = resultSet.getInt(1);
                }

                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return currentSurveyId;
    }

    public int next() {
        currentSurveyId++;

        DBMain.getInstance().asyncUpdate("INSERT IGNORE INTO SurveyDates VALUES (?, NOW());", preparedStatement -> {
            preparedStatement.setInt(1, currentSurveyId);
        });

        return currentSurveyId;
    }

    @Override
    protected void save(SurveyData surveyData) {
    }

    private HashMap<Long, SurveyFirstVote> getFirstVotes(int surveyId) {
        return new DBDataLoad<SurveyFirstVote>("SurveyVotes", "userId, personalVote, locale", "surveyId = ?",
                preparedStatement -> preparedStatement.setInt(1, surveyId)
        ).getHashMap(SurveyFirstVote::getUserId, resultSet ->
                new SurveyFirstVote(
                        resultSet.getLong(1),
                        resultSet.getByte(2),
                        new Locale(resultSet.getString(3))
                )
        );
    }

    private void addFirstVote(int surveyId, SurveyFirstVote surveyFirstVote) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO SurveyVotes (surveyId, userId, personalVote, locale) VALUES (?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, surveyId);
            preparedStatement.setLong(2, surveyFirstVote.getUserId());
            preparedStatement.setByte(3, surveyFirstVote.getVote());
            preparedStatement.setString(4, surveyFirstVote.getLocale().getDisplayName());
        });
    }

    private void removeFirstVote(int surveyId, SurveyFirstVote surveyFirstVote) {
        DBMain.getInstance().asyncUpdate("DELETE FROM SurveyVotes WHERE surveyId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, surveyId);
            preparedStatement.setLong(2, surveyFirstVote.getUserId());
        });
    }

    private HashMap<Pair<Long, Long>, SurveySecondVote> getSecondVotes(int surveyId) {
        return new DBDataLoad<SurveySecondVote>("SurveyMajorityVotes", "serverId, userId, majorityVote", "surveyId = ?",
                preparedStatement -> preparedStatement.setInt(1, surveyId)
        ).getHashMap(secondVote -> new Pair<>(secondVote.getGuildId(), secondVote.getMemberId()), resultSet -> new SurveySecondVote(resultSet.getLong(1), resultSet.getLong(2), resultSet.getByte(3)));
    }

    private void addSecondVote(int surveyId, SurveySecondVote surveySecondVote) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO SurveyMajorityVotes (surveyId, serverId, userId, majorityVote) VALUES (?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, surveyId);
            preparedStatement.setLong(2, surveySecondVote.getGuildId());
            preparedStatement.setLong(3, surveySecondVote.getMemberId());
            preparedStatement.setByte(4, surveySecondVote.getVote());
        });
    }

    private void removeSecondVote(int surveyId, SurveySecondVote surveySecondVote) {
        DBMain.getInstance().asyncUpdate("DELETE FROM SurveyVotes WHERE surveyId = ? AND serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setInt(1, surveyId);
            preparedStatement.setLong(2, surveySecondVote.getGuildId());
            preparedStatement.setLong(3, surveySecondVote.getMemberId());
        });
    }

    private ArrayList<Long> getNotificationUserIds() {
        return new DBDataLoad<Long>("SurveyNotifications", "userId", "1")
                .getArrayList(resultSet -> resultSet.getLong(1));
    }

    private void addNotificationUserId(long userId) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO SurveyNotifications (userId) VALUES (?);", preparedStatement -> {
            preparedStatement.setLong(1, userId);
        });
    }

    private void removeNotificationUserId(long userId) {
        DBMain.getInstance().asyncUpdate("DELETE FROM SurveyNotifications WHERE userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, userId);
        });
    }

}
