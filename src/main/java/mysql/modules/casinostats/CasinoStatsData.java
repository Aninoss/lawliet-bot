package mysql.modules.casinostats;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import commands.runnables.casinocategory.CasinoStatsCommand;
import core.CustomObservableList;
import core.utils.RandomUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CasinoStatsData {

    private HashMap<String, Integer> dataGames;
    private HashMap<String, Integer> dataGamesWon;
    private HashMap<String, Integer> dataGamesLost;
    private HashMap<String, BigInteger> dataCoinsWon;
    private HashMap<String, BigInteger> dataCoinsLost;

    private final CustomObservableList<CasinoStatsSlot> casinoStatsList;

    public CasinoStatsData(@NonNull List<CasinoStatsSlot> casinoStatsList) {
        this.casinoStatsList = new CustomObservableList<>(casinoStatsList);
        calculateData();
    }

    public CustomObservableList<CasinoStatsSlot> getCasinoStatsList() {
        return casinoStatsList;
    }

    public void add(String game, boolean won, long value) {
        CasinoStatsSlot casinoStatsSlot = new CasinoStatsSlot(RandomUtil.generateRandomString(30), game, won, value);
        casinoStatsList.add(casinoStatsSlot);
        calculateData();
    }

    public boolean hasData() {
        return !casinoStatsList.isEmpty();
    }

    public int getGames(String game) {
        return dataGames.getOrDefault(game, 0);
    }

    public int getGamesWon(String game) {
        return dataGamesWon.getOrDefault(game, 0);
    }

    public int getGamesLost(String game) {
        return dataGamesLost.getOrDefault(game, 0);
    }

    public int getWinRatePercent(String game) {
        int won = getGamesWon(game);
        int lost = getGamesLost(game);
        if ((won + lost) == 0) {
            return 0;
        }

        return won * 100 / (won + lost);
    }

    public BigInteger getCoinsWon(String game) {
        return dataCoinsWon.getOrDefault(game, new BigInteger("0"));
    }

    public BigInteger getCoinsLost(String game) {
        return dataCoinsLost.getOrDefault(game, new BigInteger("0"));
    }

    public long getAverageCoinsPerGame(String game) {
        BigInteger wonRemainingTotal = getCoinsWon(game).subtract(getCoinsLost(game)).multiply(new BigInteger("10"));
        int gamesTotal = getGamesWon(game) + getGamesLost(game);
        if (gamesTotal == 0) {
            return 0L;
        }

        return Math.round(wonRemainingTotal.divide(new BigInteger(String.valueOf(gamesTotal))).doubleValue() / 10.0);
    }

    private void calculateData() {
        HashMap<String, Integer> dataGames = new HashMap<>();
        HashMap<String, Integer> dataGamesWon = new HashMap<>();
        HashMap<String, Integer> dataGamesLost = new HashMap<>();
        HashMap<String, BigInteger> dataCoinsWon = new HashMap<>();
        HashMap<String, BigInteger> dataCoinsLost = new HashMap<>();

        for (CasinoStatsSlot slot : casinoStatsList) {
            increaseHashMapValueInt(dataGames, slot.getGame(), 1);
            if (slot.isWon() && slot.getValue() > 0) {
                increaseHashMapValueInt(dataGamesWon, slot.getGame(), 1);
                increaseHashMapValueBigDecimal(dataCoinsWon, slot.getGame(), new BigInteger(String.valueOf(slot.getValue())));
            } else if (!slot.isWon() && slot.getValue() > 0) {
                increaseHashMapValueInt(dataGamesLost, slot.getGame(), 1);
                increaseHashMapValueBigDecimal(dataCoinsLost, slot.getGame(), new BigInteger(String.valueOf(slot.getValue())));
            }
        }

        this.dataGames = dataGames;
        this.dataGamesWon = dataGamesWon;
        this.dataGamesLost = dataGamesLost;
        this.dataCoinsWon = dataCoinsWon;
        this.dataCoinsLost = dataCoinsLost;
    }

    private void increaseHashMapValueInt(HashMap<String, Integer> hashMap, String game, int delta) {
        int valueGame = hashMap.computeIfAbsent(game, k -> 0);
        hashMap.put(game, valueGame + delta);

        int valueAll = hashMap.computeIfAbsent(CasinoStatsCommand.SELECT_MENU_ALLGAMES_ID, k -> 0);
        hashMap.put(CasinoStatsCommand.SELECT_MENU_ALLGAMES_ID, valueAll + delta);
    }

    private void increaseHashMapValueBigDecimal(HashMap<String, BigInteger> hashMap, String game, BigInteger delta) {
        BigInteger valueGame = hashMap.computeIfAbsent(game, k -> new BigInteger("0"));
        hashMap.put(game, valueGame.add(delta));

        BigInteger valueAll = hashMap.computeIfAbsent(CasinoStatsCommand.SELECT_MENU_ALLGAMES_ID, k -> new BigInteger("0"));
        hashMap.put(CasinoStatsCommand.SELECT_MENU_ALLGAMES_ID, valueAll.add(delta));
    }

}