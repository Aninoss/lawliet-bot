package modules;

import core.SecretManager;
import core.Security;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;

public class ExchangeRate {

    private static final ExchangeRate instance = new ExchangeRate();
    private ExchangeRate() {}
    public static ExchangeRate getInstance() {
        return instance;
    }

    private final HashMap<String, Integer> rateMap = new HashMap<>();

    public int get(int offset) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, offset);
        String dateString = calendar.get(Calendar.YEAR) + ";" + calendar.get(Calendar.DAY_OF_YEAR);

        int n = rateMap.computeIfAbsent(dateString, key -> -1);
        if (n == -1) {
            int root = Security.getHashForString(SecretManager.getString("exchangerate.secret"), Security.getHashForString("DateString", dateString)).hashCode();
            Random r = new Random(root);

            double result = r.nextDouble();
            for (int i = 0; i < 3; i++) {
                double d = r.nextDouble();
                if (Math.abs(d - 0.5) < Math.abs(result - 0.5)) result = d;
            }

            n = (int) Math.round(100 * (0.5 + result * 1.0));
            rateMap.put(dateString, n);
        }

        return n;
    }

}
