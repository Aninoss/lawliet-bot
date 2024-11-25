package modules.txt2img;

public enum AspectRatio {

    SQUARE(1024, 1024, 1.5),
    LANDSCAPE(1216, 832, 1.25),
    PORTRAIT(832, 1216, 1.25);

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
