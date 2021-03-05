package modules;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import core.MainLogger;
import core.Security;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class ExchangeRate {

    private static final ExchangeRate instance = new ExchangeRate();

    private ExchangeRate() {
    }

    public static ExchangeRate getInstance() {
        return instance;
    }

    private final LoadingCache<String, Integer> rateCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofHours(6))
            .build(new CacheLoader<>() {
                @Override
                public Integer load(@NonNull String dateString) throws Exception {
                    int root = Security.getHashForString(
                            System.getenv("EX_SECRET"),
                            Security.getHashForString("DateString", dateString)
                    ).hashCode();
                    Random r = new Random(root);

                    double result = r.nextDouble();
                    for (int i = 0; i < 3; i++) {
                        double d = r.nextDouble();
                        if (Math.abs(d - 0.5) < Math.abs(result - 0.5)) result = d;
                    }

                    return (int) Math.round(100 * (0.5 + result * 1.0));
                }
            });

    public int get(int offset) throws InvalidKeySpecException, NoSuchAlgorithmException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, offset);
        String dateString = calendar.get(Calendar.YEAR) + ";" + calendar.get(Calendar.DAY_OF_YEAR);

        try {
            return rateCache.get(dateString);
        } catch (ExecutionException e) {
            MainLogger.get().error("Exception", e);
        }
        return 100;
    }

}
