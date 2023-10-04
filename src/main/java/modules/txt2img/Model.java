package modules.txt2img;

import commands.runnables.RunPodAbstract;
import commands.runnables.aitoyscategory.Txt2ImgCommand;
import commands.runnables.nsfwcategory.Txt2HentaiCommand;

import java.util.Map;
import java.util.Set;

public enum Model {

    STABLE_DIFFUSION_V1(
            "stable-diffusion-v1",
            1,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 8,
                    "num_inference_steps", 30,
                    "num_outputs", 1
            )
    ),

    STABLE_DIFFUSION_V2(
            "stable-diffusion-v2",
            1,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 8,
                    "num_inference_steps", 30,
                    "num_outputs", 1
            )
    ),

    OPENJOURNEY(
            "sd-openjourney",
            1,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 7,
                    "num_inference_steps", 30,
                    "num_outputs", 1
            )
    ),

    KANDINSKY(
            "kandinsky-v2",
            1,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "num_steps", 30,
                    "guidance_scale", 4,
                    "h", 768,
                    "w", 768,
                    "num_images", 1
            )
    ),

    ANYTHING_V3(
            "sd-anything-v3",
            1,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 6,
                    "num_inference_steps", 25,
                    "num_outputs", 1
            )
    ),

    MEINA_HENTAI(
            "7x0f7het69525n",
            0.75,
            true,
            Set.of(Txt2HentaiCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 6,
                    "num_inference_steps", 75,
                    "num_outputs", 1
            )
    ),

    BB95_FURRY_MIX(
            "i094k7b7ps3z9p",
            0.5,
            true,
            Set.of(Txt2HentaiCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 10,
                    "num_inference_steps", 75,
                    "num_outputs", 1
            )
    );


    private final String modelId;
    private final double timeMultiplier;
    private final boolean customModel;
    private final Set<Class<? extends RunPodAbstract>> classes;
    private final Map<String, Object> inputMap;

    Model(String modelId, double timeMultiplier, boolean customModel, Set<Class<? extends RunPodAbstract>> classes,
          Map<String, Object> inputMap
    ) {
        this.modelId = modelId;
        this.timeMultiplier = timeMultiplier;
        this.customModel = customModel;
        this.classes = classes;
        this.inputMap = inputMap;
    }

    public String getModelId() {
        return modelId;
    }

    public double getTimeMultiplier() {
        return timeMultiplier;
    }

    public boolean getCustomModel() {
        return customModel;
    }

    public Set<Class<? extends RunPodAbstract>> getClasses() {
        return classes;
    }

    public Map<String, Object> getInputMap() {
        return inputMap;
    }

}
