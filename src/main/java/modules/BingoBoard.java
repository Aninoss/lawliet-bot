package modules;

import java.util.Random;

public class BingoBoard {

    public static final String[] EMOJIS = new String[]{
            "🍇", "🍈", "🍉", "🥜", "🍋",
            "🍌", "🍍", "🍄", "🥒", "🍏",
            "🍑", "🍒", "🍓", "🌶️", "🥝",
            "🍅", "🥥", "🍆", "🥕", "🧄"
    };

    private final int id;
    private final int[][] values;
    private final boolean[][] marked;

    public BingoBoard(int id) {
        this.id = id;
        Random random = new Random();
        marked = new boolean[5][5];
        values = new int[5][5];
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                values[y][x] = random.nextInt(EMOJIS.length);
            }
        }
    }

    public int getId() {
        return id;
    }

    public void solve(int value) {
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                if (values[y][x] == value) {
                    marked[y][x] = true;
                }
            }
        }
    }

    public boolean completed() {
        /* check rows */
        outer:
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                if (!marked[y][x]) {
                    continue outer;
                }
            }
            return true;
        }

        /* check columns */
        outer:
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                if (!marked[y][x]) {
                    continue outer;
                }
            }
            return true;
        }

        /* check diagonal */
        return (marked[0][0] && marked[1][1] && marked[2][2] && marked[3][3] && marked[4][4]) ||
                (marked[0][4] && marked[1][3] && marked[2][2] && marked[3][1] && marked[4][0]);
    }

    public String draw() {
        StringBuilder sb = new StringBuilder();

        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                String borderString = marked[y][x] ? "||" : "";
                sb.append(borderString)
                        .append('`')
                        .append(EMOJIS[values[y][x]])
                        .append('`')
                        .append(borderString)
                        .append(' ');
            }
            sb.append('\n');
        }

        return sb.toString();
    }

}
