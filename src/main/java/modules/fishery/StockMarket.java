package modules.fishery;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import core.utils.TimeUtil;

public class StockMarket {

    public static final int FIRST_HOUR_SLOT = 452_558;
    public static final int UPDATE_SLOT = 460_970;

    private static final HashMap<String, Double> cache = new HashMap<>();
    private static final MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static long getValue(Stock stock) {
        return getValue(stock, 0);
    }

    public static long getValue(Stock stock, int relative) {
        long currentHourSlot = TimeUtil.currentHour();
        loadValuesIfAbsent(stock);
        return Math.round(cache.get(generateKey(stock, currentHourSlot + relative)));
    }

    public static void loadValuesIfAbsent(Stock stock) {
        long currentHourSlot = TimeUtil.currentHour();
        if (!cache.containsKey(generateKey(stock, currentHourSlot))) {
            String keyPrevious = generateKey(stock, currentHourSlot - 1);
            if (cache.containsKey(keyPrevious)) {
                long seed = generateSeed(stock, currentHourSlot);
                registerValue(stock, seed, currentHourSlot, cache.get(keyPrevious), currentHourSlot);
            } else {
                double value = stock.getStartingPrice();
                for (long i = FIRST_HOUR_SLOT; i <= currentHourSlot; i++) {
                    long seed = generateSeed(stock, i);
                    value = registerValue(stock, seed, i, value, currentHourSlot);
                }
            }
        }
    }

    private static String generateKey(Stock stock, long timeSlot) {
        return stock.getId() + ":" + timeSlot;
    }

    private static long generateSeed(Stock stock, long hourSlot) {
        if (hourSlot <= UPDATE_SLOT) {
            return Objects.hash(Long.parseLong(System.getenv("STOCKMARKET_TOKEN")) + Objects.hash(stock.getId()), hourSlot);
        } else {
            String content = System.getenv("STOCKMARKET_TOKEN") + ":" + stock.getId() + ":" + hourSlot;
            messageDigest.update(content.getBytes());
            return new String(messageDigest.digest()).hashCode();
        }
    }

    private static double registerValue(Stock stock, long seed, long hourSlot, double value, long currentHourSlot) {
        Random random = new Random(seed);
        double multi = 1.0 - Math.abs(random.nextGaussian()) * 0.025;
        if (hourSlot <= UPDATE_SLOT) {
            if (random.nextBoolean()) {
                multi = 1.0 / multi;
            }
        } else {
            double threshold = value > stock.getStartingPrice() ? 0.52 : 0.48;
            if (random.nextDouble() > threshold) {
                multi = 1.0 / multi;
            }
        }
        value *= multi;
        if (hourSlot >= currentHourSlot - 24 * 8) {
            cache.put(generateKey(stock, hourSlot), value);
        }
        return value;
    }

}
