package General.Fishing;

public class FishingSlot {

    private int id;
    private long level, startPrice, effect;
    private double power;

    public FishingSlot(int id, long level, long startPrice, double power, long effect) {
        this.id = id;
        this.level = level;
        this.startPrice = startPrice;
        this.effect = effect;
        this.power = power;
    }

    public long getPrice() {
        return (long) (Math.pow(getValue(level), power) * startPrice);
    }

    public long getEffect() {
        return getValue(level) * effect;
    }

    public long getDeltaEffect() {
        return (getValue(level + 1) - getValue(level)) * effect;
    }

    public static long getValue(long level) {
        long value = 0;
        for(int i = 1; i <= level + 1; i++) {
            value += i;
        }

        return value;
    }

    public int getLevel() {
        return (int) level;
    }

    public int getId() {
        return id;
    }

    public void levelUp() { level++; }

    public double getPower() {
        return power;
    }
}
