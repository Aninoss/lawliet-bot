package modules;

import java.util.Random;

public class ChatGameGuessingNames {

    private static final ChatGameGuessingNames ourInstance = new ChatGameGuessingNames();
    public static ChatGameGuessingNames getInstance() { return ourInstance; }
    private ChatGameGuessingNames() { }

    private final Random r = new Random();
    private int num = r.nextInt(100000) + 1;
    private int tries = 0;

    public int check(int val) {
        if (val == num) {
            this.num = r.nextInt(100000) + 1;
            return 0;
        }

        tries++;
        return num > val ? 1 : -1;
    }

    public int getTries() {
        return tries;
    }

}
