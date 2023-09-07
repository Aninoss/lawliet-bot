package modules.txt2img;

public enum Model {

    STABLE_DIFFUSION_V1(
            "stable-diffusion-v1",
            1,
            30,
            8,
            "K-LMS",
            1
    ),

    STABLE_DIFFUSION_V2(
            "stable-diffusion-v2",
            1,
            30,
            8,
            "KLMS",
            1
    ),

    OPENJOURNEY(
            "sd-openjourney",
            1,
            30,
            7,
            "K-LMS",
            1
    ),

    ANYTHING_V3(
            "sd-anything-v3",
            1,
            25,
            6,
            "K-LMS",
            1.25
    );


    private final String modelId;
    private final int numOutputs;
    private final int numInferenceSteps;
    private final double guidanceScale;
    private final String scheduler;
    private final double timeMultiplier;

    Model(String modelId, int numOutputs, int numInferenceSteps, double guidanceScale, String scheduler, double timeMultiplier) {
        this.modelId = modelId;
        this.numOutputs = numOutputs;
        this.numInferenceSteps = numInferenceSteps;
        this.guidanceScale = guidanceScale;
        this.scheduler = scheduler;
        this.timeMultiplier = timeMultiplier;
    }

    public String getModelId() {
        return modelId;
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

    public double getTimeMultiplier() {
        return timeMultiplier;
    }
}
