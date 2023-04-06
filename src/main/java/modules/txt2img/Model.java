package modules.txt2img;

public enum Model {

    STABLE_DIFFUSION_V1(
            "stable-diffusion-v1",
            "ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, mutation, mutated, extra limbs, extra legs, extra arms, disfigured, deformed, cross-eye, body out of frame, blurry, bad art, bad anatomy, blurred, text, watermark, grainy, kitsch, low-res, missing limb, floating limbs, disconnected limbs, malformed hands",
            1,
            30,
            8,
            "K-LMS",
            true,
            1
    ),

    STABLE_DIFFUSION_V2(
            "stable-diffusion-v2",
            "ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, mutation, mutated, extra limbs, extra legs, extra arms, disfigured, deformed, cross-eye, body out of frame, blurry, bad art, bad anatomy, blurred, text, watermark, grainy, kitsch, low-res, missing limb, floating limbs, disconnected limbs, malformed hands",
            1,
            30,
            8,
            "KLMS",
            false,
            1
    ),

    OPENJOURNEY(
            "sd-openjourney",
            "ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, mutation, mutated, extra limbs, extra legs, extra arms, disfigured, deformed, cross-eye, body out of frame, blurry, bad art, bad anatomy, blurred, text, watermark, grainy, kitsch, low-res, missing limb, floating limbs, disconnected limbs, malformed hands",
            1,
            50,
            7,
            "K-LMS",
            true,
            1
    ),

    ANYTHING_V3(
            "sd-anything-v3",
            "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, fewer digits, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, artist name",
            1,
            25,
            7,
            "K-LMS",
            true,
            1.5
    );


    private final String modelId;
    private final String negativePrompt;
    private final int numOutputs;
    private final int numInferenceSteps;
    private final double guidanceScale;
    private final String scheduler;
    private final boolean checkNsfw;
    private final double timeMultiplier;

    Model(String modelId, String negativePrompt, int numOutputs, int numInferenceSteps, double guidanceScale,
          String scheduler, boolean checkNsfw, double timeMultiplier
    ) {
        this.modelId = modelId;
        this.negativePrompt = negativePrompt;
        this.numOutputs = numOutputs;
        this.numInferenceSteps = numInferenceSteps;
        this.guidanceScale = guidanceScale;
        this.scheduler = scheduler;
        this.checkNsfw = checkNsfw;
        this.timeMultiplier = timeMultiplier;
    }

    public String getModelId() {
        return modelId;
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

    public String getScheduler() {
        return scheduler;
    }

    public boolean getCheckNsfw() {
        return checkNsfw;
    }

    public double getTimeMultiplier() {
        return timeMultiplier;
    }
}
