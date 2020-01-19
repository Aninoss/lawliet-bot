package General;

import java.util.Random;

public class GameCard {

    private int value;
    private String emoji;
    private boolean ace;

    public GameCard() {
        ace = false;
        emoji = "";
        value = 0;

        Random r = new Random();
        int card = r.nextInt(13);

        if (card <= 8) {
            value = card + 2;
            switch (value) {
                case 2:
                    emoji = ":two:";
                    break;
                case 3:
                    emoji = ":three:";
                    break;
                case 4:
                    emoji = ":four:";
                    break;
                case 5:
                    emoji = ":five:";
                    break;
                case 6:
                    emoji = ":six:";
                    break;
                case 7:
                    emoji = ":seven:";
                    break;
                case 8:
                    emoji = ":eight:";
                    break;
                case 9:
                    emoji = ":nine:";
                    break;
                default:
                    emoji = ":keycap_ten:";
                    break;
            }
        }
        if (card == 9) {
            value = 10;
            emoji = ":regional_indicator_j:";
        }
        if (card == 10) {
            value = 10;
            emoji = ":regional_indicator_q:";
        }
        if (card == 11) {
            value = 10;
            emoji = ":regional_indicator_k:";
        }
        if (card == 12) {
            value = 11;
            ace = true;
            emoji = ":a:";
        }
    }

    public int getValue() {
        return value;
    }

    public String getEmoji() {
        return emoji;
    }

    public boolean isAce() {
        return ace;
    }
}
