package modules.replicate;

public enum Model {

    OPENJOURNEY("9936c2001faa2194a261c01381f90e65261879985476014a0a37a334593a05eb", 4),
    STABLE_DIFFUSION("f178fa7a1ae43a9a9af01b833b9d2ecf97b1bcb0acfd2dc5dd04895e042863f1", 1),
    ANYTHING_V4("42a996d39a96aedc57b2e0aa8105dea39c9c89d9d266caf6bb4327a1c191b061", 1);

    private final String version;
    private final int numOutputs;

    Model(String version, int numOutputs) {
        this.version = version;
        this.numOutputs = numOutputs;
    }

    public String getVersion() {
        return version;
    }

    public int getNumOutputs() {
        return numOutputs;
    }
}
