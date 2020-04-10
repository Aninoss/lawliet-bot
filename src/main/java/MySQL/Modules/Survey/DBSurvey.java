package MySQL.Modules.Survey;

import MySQL.DBBeanGenerator;
import MySQL.DBDataLoad;
import MySQL.DBMain;
import javafx.util.Pair;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class DBSurvey extends DBBeanGenerator<Integer, SurveyBean> {

    private static DBSurvey ourInstance = new DBSurvey();
    public static DBSurvey getInstance() { return ourInstance; }
    private DBSurvey() {}

    private Integer currentSurveyId = null;

    @Override
    protected SurveyBean loadBean(Integer surveyId) throws Exception {
        SurveyBean surveyBean;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT start FROM SurveyDates WHERE surveyId = ?;");
        preparedStatement.setInt(1, surveyId);
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) {
            surveyBean = new SurveyBean(
                    surveyId,
                    resultSet.getDate(1).toLocalDate(),
                    getFirstVotes(surveyId),
                    getSecondVotes(surveyId)
            );
        } else {
            surveyBean = new SurveyBean(
                    surveyId,
                    resultSet.getDate(1).toLocalDate(),
                    new HashMap<>(),
                    new HashMap<>()
            );
        }

        resultSet.close();
        preparedStatement.close();

        surveyBean.getFirstVotes()
                .addMapAddListener(firstVote -> addFirstVote(surveyId, firstVote))
                .addMapRemoveListener(firstVote -> removeFirstVote(surveyId, firstVote));
        surveyBean.getSecondVotes()
                .addMapAddListener(secondVote -> addSecondVote(surveyId, secondVote))
                .addMapRemoveListener(secondVote -> removeSecondVote(surveyId, secondVote));

        return surveyBean;
    }

    public SurveyBean getCurrentSurvey() throws SQLException, ExecutionException {
        return getBean(getCurrentSurveyId());
    }

    public synchronized int getCurrentSurveyId() throws SQLException {
        if (currentSurveyId == null) {
            Statement statement = DBMain.getInstance().statement("SELECT surveyId FROM SurveyDates ORDER BY start DESC LIMIT 1;");
            ResultSet resultSet = statement.getResultSet();

            if (resultSet.next())
                currentSurveyId = resultSet.getInt(1);

            resultSet.close();
            statement.close();
        }

        return currentSurveyId;
    }

    public int next() {
        currentSurveyId++;

        DBMain.getInstance().asyncUpdate("INSERT INTO SurveyDates VALUES (?, NOW());", preparedStatement -> {
            preparedStatement.setInt(1, currentSurveyId);
        });

        return currentSurveyId;
    }

    @Override
    protected void saveBean(SurveyBean surveyBean) {}

    private HashMap<Long, SurveyFirstVote> getFirstVotes(int surveyId) throws SQLException {
        return new DBDataLoad<SurveyFirstVote>("SurveyVotes", "userId, personalVote", "surveyId = ?",
                preparedStatement -> preparedStatement.setInt(1, surveyId)
        ).getHashMap(SurveyFirstVote::getUserId, resultSet -> new SurveyFirstVote(resultSet.getLong(1), resultSet.getByte(2)));
    }

    private void addFirstVote(int surveyId, SurveyFirstVote surveyFirstVote) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO SurveyVotes (surveyId, userId, personalVote) VALUES (?, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, surveyId);
            preparedStatement.setLong(2, surveyFirstVote.getUserId());
            preparedStatement.setByte(3, surveyFirstVote.getVote());
        });
    }

    private void removeFirstVote(int surveyId, SurveyFirstVote surveyFirstVote) {
        DBMain.getInstance().asyncUpdate("DELETE FROM SurveyVotes WHERE surveyId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setLong(1, surveyId);
            preparedStatement.setLong(2, surveyFirstVote.getUserId());
        });
    }

    private HashMap<Pair<Long, Long>, SurveySecondVote> getSecondVotes(int surveyId) throws SQLException {
        return new DBDataLoad<SurveySecondVote>("SurveyMajorityVotes", "serverId, userId, majorityVote", "surveyId = ?",
            preparedStatement -> preparedStatement.setInt(1, surveyId)
        ).getHashMap(secondVote -> new Pair<>(secondVote.getServerId(), secondVote.getUserId()), resultSet -> new SurveySecondVote(resultSet.getLong(1), resultSet.getLong(2), resultSet.getByte(3)));
    }

    private void addSecondVote(int surveyId, SurveySecondVote surveySecondVote) {
        DBMain.getInstance().asyncUpdate("REPLACE INTO SurveyMajorityVotes (surveyId, serverId, userId, majorityVote) VALUES (?, ?, ?, ?);", preparedStatement -> {
            preparedStatement.setInt(1, surveyId);
            preparedStatement.setLong(2, surveySecondVote.getServerId());
            preparedStatement.setLong(3, surveySecondVote.getUserId());
            preparedStatement.setByte(4, surveySecondVote.getVote());
        });
    }

    private void removeSecondVote(int surveyId, SurveySecondVote surveySecondVote) {
        DBMain.getInstance().asyncUpdate("DELETE FROM SurveyVotes WHERE surveyId = ? AND serverId = ? AND userId = ?;", preparedStatement -> {
            preparedStatement.setInt(1, surveyId);
            preparedStatement.setLong(2, surveySecondVote.getServerId());
            preparedStatement.setLong(3, surveySecondVote.getUserId());
        });
    }

}
