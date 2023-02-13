package modules.replicate;

public enum Model {

    STABLE_DIFFUSION(
            "f178fa7a1ae43a9a9af01b833b9d2ecf97b1bcb0acfd2dc5dd04895e042863f1",
            "",
            "ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, mutation, mutated, extra limbs, extra legs, extra arms, disfigured, deformed, cross-eye, body out of frame, blurry, bad art, bad anatomy, blurred, text, watermark, grainy, kitsch, low-res, missing limb, floating limbs, disconnected limbs, malformed hands",
            1,
            20,
            7.5,
            false
    ),

    OPENJOURNEY(
            "9936c2001faa2194a261c01381f90e65261879985476014a0a37a334593a05eb",
            "mdjrny-v4 style ",
            "ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, mutation, mutated, extra limbs, extra legs, extra arms, disfigured, deformed, cross-eye, body out of frame, blurry, bad art, bad anatomy, blurred, text, watermark, grainy, kitsch, low-res, missing limb, floating limbs, disconnected limbs, malformed hands",
            4,
            20,
            7,
            true
    ),

    ANYTHING_V3(
            "09a5805203f4c12da649ec1923bb7729517ca25fcac790e640eaa9ed66573b65",
            "",
            "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, artist name",
            1,
            15,
            7,
            false
    ),

    ANYTHING_V4(
            "42a996d39a96aedc57b2e0aa8105dea39c9c89d9d266caf6bb4327a1c191b061",
            "",
            "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, artist name",
            4,
            15,
            7,
            false
    );


    private final String version;
    private final String prefix;
    private final String negativePrompt;
    private final int numOutputs;
    private final int numInferenceSteps;
    private final double guidanceScale;
    private final boolean checkNsfw;

    Model(String version, String prefix, String negativePrompt, int numOutputs, int numInferenceSteps, double guidanceScale, boolean checkNsfw) {
        this.version = version;
        this.prefix = prefix;
        this.negativePrompt = negativePrompt;
        this.numOutputs = numOutputs;
        this.numInferenceSteps = numInferenceSteps;
        this.guidanceScale = guidanceScale;
        this.checkNsfw = checkNsfw;
    }

    public String getVersion() {
        return version;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getNegativePrompt() {
        return negativePrompt;
    }

    public int getNumOutputs() {
        return numOutputs;
    }

    public int getNumInferenceSteps() {
        return numInferenceSteps;
    }

    public double getGuidanceScale() {
        return guidanceScale;
    }

    public boolean getCheckNsfw() {
        return checkNsfw;
    }
}
