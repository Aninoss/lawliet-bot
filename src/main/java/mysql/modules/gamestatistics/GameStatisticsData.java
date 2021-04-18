package mysql.modules.gamestatistics;

import java.util.Observable;

public class GameStatisticsData extends Observable {

    private final String command;
    private final double[] values;

    public GameStatisticsData(String command, double[] values) {
        this.command = command;
        this.values = values;
    }

    public void addValue(boolean won, double add) {
        if (add > 0) {
            synchronized (this) {
                values[won ? 1 : 0] += add;
            }
            setChanged();
            notifyObservers();
        }
    }

    public String getCommand() {
        return command;
    }

    public synchronized double getValue(boolean won) {
        return values[won ? 1 : 0];
    }

}