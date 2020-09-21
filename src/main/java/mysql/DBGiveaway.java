package mysql;

import javafx.util.Pair;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DBGiveaway {

    public static boolean registerGiveaway(Server server, User user) throws SQLException {
        boolean quit = false;

        PreparedStatement preparedStatement = DBMain.getInstance().preparedStatement("SELECT * FROM Giveaway WHERE serverId = ? AND userId = ?;");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.execute();

        ResultSet resultSet = preparedStatement.getResultSet();
        if (resultSet.next()) quit = true;

        resultSet.close();
        preparedStatement.close();

        if (quit) return false;

        preparedStatement = DBMain.getInstance().preparedStatement("INSERT IGNORE INTO Giveaway (serverId, userId) VALUES (?, ?);");
        preparedStatement.setLong(1, server.getId());
        preparedStatement.setLong(2, user.getId());
        preparedStatement.execute();

        return true;
    }

    public static ArrayList<Pair<Long, Long>> getGiveawaySlots() throws SQLException {
        ArrayList<Pair<Long, Long>> slots = new ArrayList<>();

        Statement statement = DBMain.getInstance().statementExecuted("SELECT serverId, userId FROM Giveaway ORDER BY RAND();");
        ResultSet resultSet = statement.getResultSet();
        while(resultSet.next())
            slots.add(new Pair<>(resultSet.getLong(1), resultSet.getLong(2)));

        resultSet.close();
        statement.close();

        return slots;
    }

}
