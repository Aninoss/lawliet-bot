package modules.replicate;

public enum Model {

    STABLE_DIFFUSION("f178fa7a1ae43a9a9af01b833b9d2ecf97b1bcb0acfd2dc5dd04895e042863f1", "", 1, false),
    OPENJOURNEY("9936c2001faa2194a261c01381f90e65261879985476014a0a37a334593a05eb", "mdjrny-v4 style ", 1, true),
    ANYTHING_V4("42a996d39a96aedc57b2e0aa8105dea39c9c89d9d266caf6bb4327a1c191b061", "", 4, false);

    private final String version;
    private final String prefix;
    private final int numOutputs;
    private final boolean checkNsfw;

    Model(String version, String prefix, int numOutputs, boolean checkNsfw) {
        this.version = version;
        this.prefix = prefix;
        this.numOutputs = numOutputs;
        this.checkNsfw = checkNsfw;
    }

    public String getVersion() {
        return version;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getNumOutputs() {
        return numOutputs;
    }

    public boolean getCheckNsfw() {
        return checkNsfw;
    }
}
