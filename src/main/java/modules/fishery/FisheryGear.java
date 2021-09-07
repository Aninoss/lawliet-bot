package modules.fishery;

public enum FisheryGear {

    MESSAGE("🎣", 25000, 1),
    DAILY("🤖", 25000, 100),
    VOICE("🥅", 32000, 1),
    TREASURE("🔍", 20000, 80000),
    ROLE("🏷", 50000, 0),
    SURVEY("🗳️", 19000, 60000),
    WORK("💼", 25000, 4000);

    private final String emoji;
    private final int startPrice;
    private final int effect;

    FisheryGear(String emoji, int startPrice, int effect) {
        this.emoji = emoji;
        this.startPrice = startPrice;
        this.effect = effect;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getStartPrice() {
        return startPrice;
    }

    public int getEffect() {
        return effect;
    }

    public static FisheryGear parse(String str) {
        return switch (str.toLowerCase()) {
            case "fishingrod", "rod", "message", "messages" -> FisheryGear.MESSAGE;
            case "fishingrobot", "robot", "fishingbot", "bot", "day", "daily", "dailies" -> FisheryGear.DAILY;
            case "fishingnet", "net", "vc", "voice", "voicechannel", "voicechannels" -> FisheryGear.VOICE;
            case "metal", "detector", "detectors", "metaldetector", "metaldetectors", "treasurechest", "treasurechests", "chest", "chests" -> FisheryGear.TREASURE;
            case "role", "roles", "buyablerole", "buyableroles", "fisheryrole", "fisheryroles" -> FisheryGear.ROLE;
            case "survey", "surveys" -> FisheryGear.SURVEY;
            case "work", "working", "salary" -> FisheryGear.WORK;
            default -> null;
        };
    }

}