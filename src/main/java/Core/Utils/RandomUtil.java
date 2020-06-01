package Core.Utils;

import java.util.ArrayList;
import java.util.Random;

public final class RandomUtil {

    public static String randomUpperCase(String answer) {
        StringBuilder sb = new StringBuilder();
        Random n = new Random();
        for (char c : answer.toCharArray()) {
            if (n.nextBoolean()) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

}
