package modules.txt2img;

public enum AspectRatio {

    SQUARE(512, 512, 1.5, 1024, 1024),
    LANDSCAPE(768, 512, 1.25, 1216, 832),
    PORTRAIT(512, 768, 1.25, 832, 1216);

    private final int width;
    private final int height;
    private final double scaling;
    private final int widthXL;
    private final int heightXL;

    AspectRatio(int width, int height, double scaling, int widthXL, int heightXL) {
        this.width = width;
        this.height = height;
        this.scaling = scaling;
        this.widthXL = widthXL;
        this.heightXL = heightXL;
    }

    public int getWidth(boolean xl) {
        return xl ? widthXL : width;
    }

    public int getHeight(boolean xl) {
        return xl ? heightXL : height;
    }

    public double getScaling() {
        return scaling;
    }
}
