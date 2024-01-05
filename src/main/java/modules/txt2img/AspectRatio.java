package modules.txt2img;

public enum AspectRatio {

    SQUARE(512, 512, 1.5),
    LANDSCAPE(768, 512, 1.25),
    PORTRAIT(512, 768, 1.25);

    private final int width;
    private final int height;
    private final double scaling;

    AspectRatio(int width, int height, double scaling) {
        this.width = width;
        this.height = height;
        this.scaling = scaling;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getScaling() {
        return scaling;
    }
}
