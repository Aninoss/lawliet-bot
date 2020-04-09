package Core.Tools;

import java.util.ArrayList;
import java.util.Random;

public class RandomTools {

    public static int pickFullRandom(ArrayList<Integer> usedSlots, int size) {
        Random n = new Random();
        int i;
        do {
            i = n.nextInt(size);
        } while (usedSlots.contains(i));
        usedSlots.add(i);
        if (usedSlots.size() == size)
            usedSlots.remove(0);
        return i;
    }

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
