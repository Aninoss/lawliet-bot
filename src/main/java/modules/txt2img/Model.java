package modules.txt2img;

import java.util.Map;

public enum Model {

    STABLE_DIFFUSION_V1(
            "stable-diffusion-v1",
            1,
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 8,
                    "num_inference_steps", 30,
                    "num_outputs", 1,
                    "scheduler", "KLMS"
            )
    ),

    STABLE_DIFFUSION_V2(
            "stable-diffusion-v2",
            1,
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 8,
                    "num_inference_steps", 30,
                    "num_outputs", 1,
                    "scheduler", "KLMS"
            )
    ),

    OPENJOURNEY(
            "sd-openjourney",
            1,
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 7,
                    "num_inference_steps", 30,
                    "num_outputs", 1,
                    "scheduler", "K-LMS"
            )
    ),

    KANDINSKY(
            "kandinsky-v2",
            1,
            Map.of(
                    "num_steps", 30,
                    "guidance_scale", 4,
                    "h", 768,
                    "w", 768,
                    "sampler", "ddim",
                    "num_images", 1
            )
    ),

    ANYTHING_V3(
            "sd-anything-v3",
            1.25,
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 6,
                    "num_inference_steps", 25,
                    "num_outputs", 1,
                    "scheduler", "K-LMS"
            )
    );


    private final String modelId;
    private final double timeMultiplier;
    private final Map<String, Object> inputMap;

    Model(String modelId, double timeMultiplier, Map<String, Object> inputMap) {
        this.modelId = modelId;
        this.timeMultiplier = timeMultiplier;
        this.inputMap = inputMap;
    }

    public String getModelId() {
        return modelId;
    }

    public double getTimeMultiplier() {
        return timeMultiplier;
    }

    public Map<String, Object> getInputMap() {
        return inputMap;
    }

}
