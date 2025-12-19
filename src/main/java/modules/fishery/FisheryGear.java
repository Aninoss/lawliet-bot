package modules.fishery;

import constants.Settings;
import core.utils.EmojiUtil;

public enum FisheryGear {

    MESSAGE("🎣", 25000, 1),
    DAILY("🤖", 25000, 100),
    VOICE("🥅", 32000, 1),
    TREASURE("🔍", 20000, 80000),
    ROLE("🏷", 50000, 0),
    SURVEY("🗳️", 19000, 60000),
    WORK("💼", 25000, 4000),
    PRESTIGE("⬆️", Settings.FISHERY_MAX, 0, false);

    private final String emoji;
    private final long startPrice;
    private final int effect;
    private final boolean dynamicPrice;

    FisheryGear(String emoji, long startPrice, int effect) {
        this(emoji, startPrice, effect, true);
    }

    FisheryGear(String emoji, long startPrice, int effect, boolean dynamicPrice) {
        this.emoji = emoji;
        this.startPrice = startPrice;
        this.effect = effect;
        this.dynamicPrice = dynamicPrice;
    }

    public String getEmoji() {
        return EmojiUtil.getEmojiFromOverride(emoji, name());
    }

    public long getStartPrice() {
        return startPrice;
    }

    public int getEffect() {
        return effect;
    }

    public boolean hasDynamicPrice() {
        return dynamicPrice;
    }

    public static FisheryGear parse(String str) {
        return switch (str.toLowerCase()) {
            case "fishing_rod", "fishingrod", "rod", "message", "messages" -> FisheryGear.MESSAGE;
            case "fishing_robot", "fishingrobot", "robot", "fishingbot", "bot", "day", "daily", "dailies" -> FisheryGear.DAILY;
            case "fishing_net", "fishingnet", "net", "vc", "voice", "voicechannel", "voicechannels" -> FisheryGear.VOICE;
            case "metal", "detector", "detectors", "metal_detector", "metaldetector", "metaldetectors", "treasurechest", "treasurechests", "chest", "chests" -> FisheryGear.TREASURE;
            case "role", "roles", "buyablerole", "buyableroles", "fisheryrole", "fisheryroles" -> FisheryGear.ROLE;
            case "survey", "surveys" -> FisheryGear.SURVEY;
            case "work", "working", "salary" -> FisheryGear.WORK;
            case "prestige" -> FisheryGear.PRESTIGE;
            default -> null;
        };
    }

}