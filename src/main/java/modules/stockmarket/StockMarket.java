package modules.stockmarket;

import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import core.utils.TimeUtil;

public class StockMarket {

    public static final int FIRST_HOUR_SLOT = 452_558;

    private static final HashMap<Integer, Double> cache = new HashMap<>();

    public static long getValue(Stock stock) {
        return getValue(stock, 0);
    }

    public static long getValue(Stock stock, int relative) {
        long currentHourSlot = TimeUtil.currentHour();
        loadValuesIfAbsent(stock);
        return Math.round(cache.get(Objects.hash(stock, currentHourSlot + relative)));
    }

    public static void loadValuesIfAbsent(Stock stock) {
        long currentHourSlot = TimeUtil.currentHour();
        if (!cache.containsKey(Objects.hash(stock, currentHourSlot))) {
            long seed = Long.parseLong(System.getenv("STOCKMARKET_TOKEN")) + Objects.hash(stock.getId());
            int hashPrevious = Objects.hash(stock, currentHourSlot - 1);
            if (cache.containsKey(hashPrevious)) {
                registerValue(stock, seed, currentHourSlot, cache.get(hashPrevious), currentHourSlot);
            } else {
                double value = stock.getStartingPrice();
                for (long i = FIRST_HOUR_SLOT; i <= currentHourSlot; i++) {
                    value = registerValue(stock, seed, i, value, currentHourSlot);
                }
            }
        }
    }

    private static double registerValue(Stock stock, long seed, long hourSlot, double value, long currentHourSlot) {
        Random random = new Random(Objects.hash(seed, hourSlot));
        double multi = 1.0 - Math.abs(random.nextGaussian()) * 0.025;
        if (random.nextBoolean()) {
            multi = 1.0 / multi;
        }
        value *= multi;
        if (hourSlot >= currentHourSlot - 24 * 8) {
            cache.put(Objects.hash(stock, hourSlot), value);
        }
        return value;
    }

}
