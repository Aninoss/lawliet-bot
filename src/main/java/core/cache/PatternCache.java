package core.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class PatternCache {

    private static final PatternCache ourInstance = new PatternCache();
    public static PatternCache getInstance() { return ourInstance; }
    private PatternCache() { }

    private final Cache<String, Pattern> patternMap = CacheBuilder.newBuilder().build();

    public Pattern generate(String pattern) {
        try {
            return patternMap.get(pattern, () -> Pattern.compile(pattern));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
