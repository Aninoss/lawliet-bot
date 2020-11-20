package core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class RegexPatternCache {

    private static final RegexPatternCache ourInstance = new RegexPatternCache();
    public static RegexPatternCache getInstance() { return ourInstance; }
    private RegexPatternCache() { }

    private final Cache<String, Pattern> patternMap = CacheBuilder.newBuilder().build();

    public Pattern generate(String pattern) {
        try {
            return patternMap.get(pattern, () -> Pattern.compile(pattern));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
