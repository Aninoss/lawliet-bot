package core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import mysql.MySQLManager;
import mysql.RedisManager;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
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

    public static JSONObject collectRedisData(long userId) {
        MainLogger.get().info("Generating Redis data JSON for {}", userId);

        JSONObject jsonObject = new JSONObject();
        RedisManager.update(jedis -> {
            for (long guildId : DBFishery.getInstance().getGuildIdsForFisheryUser(userId)) {
                FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(guildId);
                FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(userId);

                JSONObject guildMemberJsonObject = new JSONObject();
                for (Map.Entry<String, String> entry : RedisManager.hscan(jedis, fisheryMemberData.KEY_ACCOUNT)) {
                    guildMemberJsonObject.put(entry.getKey(), entry.getValue());
                }
                jsonObject.put(fisheryMemberData.KEY_ACCOUNT, guildMemberJsonObject);

                Double fishGains = jedis.zscore(fisheryGuildData.KEY_RECENT_FISH_GAINS_PROCESSED, String.valueOf(userId));
                JSONObject fishGainsJson = new JSONObject();
                if (fishGains != null) {
                    fishGainsJson.put(String.valueOf(userId), fishGains.toString());
                }
                jsonObject.put(fisheryGuildData.KEY_RECENT_FISH_GAINS_PROCESSED, fishGainsJson);

                JSONObject fishGainsRawJson = new JSONObject();
                List<Map.Entry<String, String>> list = RedisManager.hscan(jedis, fisheryGuildData.KEY_RECENT_FISH_GAINS_RAW);
                for (Map.Entry<String, String> entry : list) {
                    if (entry.getKey().contains(String.valueOf(userId))) {
                        fishGainsRawJson.put(entry.getKey(), entry.getValue());
                    }
                }
                jsonObject.put(fisheryGuildData.KEY_RECENT_FISH_GAINS_RAW, fishGainsRawJson);
            }
        });

        return jsonObject;
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

        if (table.equals("StaticReactionMessages")) {
            MySQLManager.get("SELECT * FROM `StaticReactionMessages` WHERE `secondaryId` LIKE ?;", ps -> {
                ps.setString(1, "%" + userId + "%");
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

    private static JSONObject extractMySQLRow(ResultSet resultSet, List<String> columns) throws SQLException {
        JSONObject tableJsonObject = new JSONObject();
        for (String s : columns) {
            tableJsonObject.put(s, resultSet.getString(s));
        }
        return tableJsonObject;
    }

}
