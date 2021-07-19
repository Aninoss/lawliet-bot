package mysql;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import redis.clients.jedis.*;

public class DBRedis {

    private static final DBRedis ourInstance = new DBRedis();

    public static DBRedis getInstance() { return ourInstance; }

    private final JedisPool jedisPool = new JedisPool(
            buildPoolConfig(),
            System.getenv("REDIS_HOST"),
            Integer.parseInt(System.getenv("REDIS_PORT"))
    );

    private DBRedis() { }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(32);
        poolConfig.setMaxIdle(32);
        poolConfig.setMinIdle(0);
        return poolConfig;
    }

    public <T> T get(Function<Jedis, T> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            return function.apply(jedis);
        }
    }

    public <T> T get(Function<Jedis, T> function, T onNull) {
        try (Jedis jedis = jedisPool.getResource()) {
            T response = function.apply(jedis);
            return response == null ? onNull : response;
        }
    }

    public int getInteger(Function<Jedis, String> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            String response = function.apply(jedis);
            return parseInteger(response);
        }
    }

    public long getLong(Function<Jedis, String> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            String response = function.apply(jedis);
            return parseLong(response);
        }
    }

    public boolean getBoolean(Function<Jedis, String> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            String response = function.apply(jedis);
            return parseBoolean(response);
        }
    }

    public LocalDate getLocalDate(Function<Jedis, String> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            String response = function.apply(jedis);
            return parseLocalDate(response);
        }
    }

    public Instant getInstant(Function<Jedis, String> function) {
        try (Jedis jedis = jedisPool.getResource()) {
            String response = function.apply(jedis);
            return parseInstant(response);
        }
    }

    public void update(Consumer<Jedis> consumer) {
        try (Jedis jedis = jedisPool.getResource()) {
            consumer.accept(jedis);
        }
    }

    public List<String> scan(Jedis jedis) {
        return scan(jedis, null);
    }

    public List<String> scan(Jedis jedis, String pattern) {
        ArrayList<String> keys = new ArrayList<>();
        ScanParams scanParams = new ScanParams().count(100);
        if (pattern != null) {
            scanParams.match(pattern);
        }
        String cur = ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<String> scanResult = jedis.scan(cur, scanParams);
            keys.addAll(scanResult.getResult());
            cur = scanResult.getCursor();
        } while (!cur.equals(ScanParams.SCAN_POINTER_START));
        return keys;
    }

    public List<Map.Entry<String, String>> hscan(Jedis jedis, String key) {
        return hscan(jedis, key, null);
    }

    public List<Map.Entry<String, String>> hscan(Jedis jedis, String key, String pattern) {
        ArrayList<Map.Entry<String, String>> keys = new ArrayList<>();
        ScanParams scanParams = new ScanParams().count(100);
        if (pattern != null) {
            scanParams.match(pattern);
        }
        String cur = ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<Map.Entry<String, String>> scanResult = jedis.hscan(key, cur, scanParams);
            keys.addAll(scanResult.getResult());
            cur = scanResult.getCursor();
        } while (!cur.equals(ScanParams.SCAN_POINTER_START));
        return keys;
    }

    public List<Tuple> zscan(Jedis jedis, String key) {
        return zscan(jedis, key, null);
    }

    public List<Tuple> zscan(Jedis jedis, String key, String pattern) {
        ArrayList<Tuple> keys = new ArrayList<>();
        ScanParams scanParams = new ScanParams().count(100);
        if (pattern != null) {
            scanParams.match(pattern);
        }
        String cur = ScanParams.SCAN_POINTER_START;
        do {
            ScanResult<Tuple> scanResult = jedis.zscan(key, cur, scanParams);
            keys.addAll(scanResult.getResult());
            cur = scanResult.getCursor();
        } while (!cur.equals(ScanParams.SCAN_POINTER_START));
        return keys;
    }

    public static long parseLong(String str) {
        if (str == null) {
            return 0L;
        } else {
            return Long.parseLong(str);
        }
    }

    public static long parseLong(Double d) {
        if (d == null) {
            return 0L;
        } else {
            return d.longValue();
        }
    }

    public static int parseInteger(Long l) {
        if (l == null) {
            return 0;
        } else {
            return l.intValue();
        }
    }

    public static int parseInteger(String str) {
        if (str == null) {
            return 0;
        } else {
            return Integer.parseInt(str);
        }
    }

    public static boolean parseBoolean(String str) {
        if (str == null) {
            return false;
        } else {
            return Boolean.parseBoolean(str);
        }
    }

    public static LocalDate parseLocalDate(String str) {
        if (str == null) {
            return null;
        } else {
            return LocalDate.parse(str);
        }
    }

    public static Instant parseInstant(String str) {
        if (str == null) {
            return null;
        } else {
            return Instant.parse(str);
        }
    }

}
