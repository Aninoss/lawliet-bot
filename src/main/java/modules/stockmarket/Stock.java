package modules.stockmarket;

public enum Stock {

    MCFISH(0, "McFish", 200),
    COCA_COAST(1, "Coca-Coast", 1500),
    MICROSQUISHY(2, "Microsquishy", 40000),
    OWOZON(3, "OwOzon", 550000),
    FINBOOK(4, "Finbook", 10000000);

    private final int id;
    private final String name;
    private final double startingPrice;

    Stock(int id, String name, double startingPrice) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

}
