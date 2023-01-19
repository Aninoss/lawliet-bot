package core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import mysql.MySQLManager;
import org.json.JSONArray;
import org.json.JSONObject;

public class GDPR {

    public static JSONObject collectMySQLData(long userId, String userTag) throws SQLException, InterruptedException {
        MainLogger.get().info("Generating MySQL data JSON for {}", userId);

        return MySQLManager.get("SHOW TABLES;", resultSet -> {
            JSONObject jsonObject = new JSONObject();
            while (resultSet.next()) {
                String table = resultSet.getString(1);
                MainLogger.get().info("Processing table {}", table);
                try {
                    JSONArray tableRowsJson = collectMySQLDataFromTable(table, userId, userTag);
                    if (!tableRowsJson.isEmpty()) {
                        jsonObject.put(table, tableRowsJson);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return jsonObject;
        });
    }

    private static JSONArray collectMySQLDataFromTable(String table, long userId, String userTag) throws SQLException, InterruptedException {
        ArrayList<String> columns = new ArrayList<>();
        HashSet<String> usefulColumns = new HashSet<>();
        HashSet<String> rowsSet = new HashSet<>();

        MySQLManager.get("SHOW COLUMNS FROM `" + table + "`;", resultSet -> {
            while (resultSet.next()) {
                String column = resultSet.getString(1);
                columns.add(column);
                if (resultSet.getString(2).equals("bigint unsigned")) {
                    usefulColumns.add(column);
                }
            }
            return null;
        });

        for (String column : columns) {
            if (usefulColumns.contains(column)) {
                MySQLManager.get("SELECT * FROM `" + table + "` WHERE `" + column + "` = ?;", ps -> {
                    ps.setLong(1, userId);
                }, resultSet -> {
                    while (resultSet.next()) {
                        rowsSet.add(extractMySQLRow(resultSet, columns).toString());
                    }
                    return null;
                });
            }
        }

        if (table.equals("SuggestionMessages")) {
            MySQLManager.get("SELECT * FROM `SuggestionMessages` WHERE `author` = ?;", ps -> {
                ps.setString(1, userTag);
            }, resultSet -> {
                while (resultSet.next()) {
                    rowsSet.add(extractMySQLRow(resultSet, columns).toString());
                }
                return null;
            });
        }

        JSONArray rowsJsonArray = new JSONArray();
        rowsSet.forEach(row -> rowsJsonArray.put(new JSONObject(row)));
        return rowsJsonArray;
    }

    private static JSONObject  extractMySQLRow(ResultSet resultSet, List<String> columns) throws SQLException {
        JSONObject tableJsonObject = new JSONObject();
        for (String s : columns) {
            tableJsonObject.put(s, resultSet.getString(s));
        }
        return tableJsonObject;
    }

}
