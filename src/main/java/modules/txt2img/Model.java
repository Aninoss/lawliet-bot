package modules.txt2img;

import commands.runnables.RunPodAbstract;
import commands.runnables.aitoyscategory.Txt2ImgCommand;
import commands.runnables.nsfwcategory.Txt2HentaiCommand;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

public enum Model {

    STABLE_DIFFUSION_V1(
            "stable-diffusion-v1",
            5500,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 8,
                    "num_inference_steps", 30,
                    "num_outputs", RunPodDownloader.PLACEHOLDER_IMAGES
            )
    ),

    STABLE_DIFFUSION_V2(
            "stable-diffusion-v2",
            5500,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 8,
                    "num_inference_steps", 30,
                    "num_outputs", RunPodDownloader.PLACEHOLDER_IMAGES
            )
    ),

    OPENJOURNEY(
            "sd-openjourney",
            5500,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 7,
                    "num_inference_steps", 30,
                    "num_outputs", RunPodDownloader.PLACEHOLDER_IMAGES
            )
    ),

    KANDINSKY(
            "kandinsky-v2",
            5500,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "num_steps", 30,
                    "guidance_scale", 4,
                    "h", 768,
                    "w", 768,
                    "num_images", RunPodDownloader.PLACEHOLDER_IMAGES
            )
    ),

    ANYTHING_V3(
            "sd-anything-v3",
            5600,
            false,
            Set.of(Txt2ImgCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "guidance_scale", 6,
                    "num_inference_steps", 25,
                    "num_outputs", RunPodDownloader.PLACEHOLDER_IMAGES
            )
    ),

    MEINA_HENTAI(
            "92mzor4a45vkc1",
            9000,
            true,
            Set.of(Txt2HentaiCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "cfg_scale", 7,
                    "steps", 30,
                    "batch_size", RunPodDownloader.PLACEHOLDER_IMAGES,
                    "sampler_name", "DPM++ SDE Karras",
                    "override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2)
            )
    ),

    BB95_FURRY_MIX(
            "7lh2avgf34pryg",
            9000,
            true,
            Set.of(Txt2HentaiCommand.class),
            Map.of(
                    "width", 768,
                    "height", 768,
                    "cfg_scale", 10,
                    "steps", 50,
                    "batch_size", RunPodDownloader.PLACEHOLDER_IMAGES,
                    "sampler_name", "Euler a"
            )
    );

    private final String modelId;
    private final int expectedTimeMs;
    private final boolean customModel;
    private final Set<Class<? extends RunPodAbstract>> classes;
    private final Map<String, Object> inputMap;

    Model(String modelId, int expectedTimeMs, boolean customModel, Set<Class<? extends RunPodAbstract>> classes,
          Map<String, Object> inputMap
    ) {
        this.modelId = modelId;
        this.expectedTimeMs = expectedTimeMs;
        this.customModel = customModel;
        this.classes = classes;
        this.inputMap = inputMap;
    }

    public String getModelId() {
        return modelId;
    }

    public double getTimeMultiplier() {
        return 5.5 / expectedTimeMs;
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
