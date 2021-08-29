package modules;

import java.util.Random;

public class ChatGameGuessingNames {

    private final Random r = new Random();
    private int num = r.nextInt(100000) + 1;
    private int tries = 0;

    public int check(int val) {
        if (val == num) {
            this.num = r.nextInt(100000) + 1;
            tries = 0;
            return 0;
        }

        tries++;
        return num > val ? 1 : -1;
    }

    public int getTries() {
        return tries;
    }

}
