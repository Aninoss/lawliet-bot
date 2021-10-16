package core.utils;

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

    public static int pickWithProbabilities(double... probabilities) {
        double value = new Random().nextDouble();
        for (int i = 0; i < probabilities.length; i++) {
            value -= probabilities[i];
            if (value < 0) {
                return i;
            }
        }

        return probabilities.length;
    }

    public static String generateRandomString(int length) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

}
