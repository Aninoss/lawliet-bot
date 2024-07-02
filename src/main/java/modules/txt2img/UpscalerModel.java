package modules.txt2img;

public enum UpscalerModel {

    R_ESRGAN("R-ESRGAN 4x+"),
    NMKD_SIAX("4x_NMKD-Siax_200k"),
    NMKD_ULTRAYANDERE("4x_NMKD-UltraYandere_300k");

    private final String name;

    UpscalerModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
