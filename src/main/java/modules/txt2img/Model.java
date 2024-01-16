package modules.txt2img;

import commands.runnables.RunPodAbstract;
import commands.runnables.aitoyscategory.Txt2ImgCommand;
import commands.runnables.nsfwcategory.Txt2HentaiCommand;
import org.json.JSONObject;

import java.util.Set;
import java.util.function.Function;

public enum Model {

    REALISTIC_VISION(
            "f3xzap2iaijkd2",
            9200,
            true,
            Set.of(Txt2ImgCommand.class),
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 4)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("enable_hr", true)
                    .put("hr_scale", params.aspectRatio.getScaling())
                    .put("hr_upscaler", "4x-UltraSharp")
                    .put("denoising_strength", "0.3")
    ),

    DREAMSHAPER(
            "0osgqg6v71hsmk",
            5800,
            true,
            Set.of(Txt2ImgCommand.class),
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 5)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
                    .put("enable_hr", true)
                    .put("hr_scale", params.aspectRatio.getScaling())
                    .put("hr_upscaler", "R-ESRGAN 4x+ Anime6B")
                    .put("hr_second_pass_steps", 10)
                    .put("denoising_strength", "0.3")
    ),

    MEINAMIX(
            "yj8156feamkasn",
            9000,
            true,
            Set.of(Txt2ImgCommand.class),
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 7)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "DPM++ SDE Karras")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
                    .put("enable_hr", true)
                    .put("hr_scale", params.aspectRatio.getScaling())
                    .put("hr_upscaler", "R-ESRGAN 4x+ Anime6B")
                    .put("hr_second_pass_steps", 10)
                    .put("denoising_strength", "0.3")
    ),

    GHOSTMIX(
            "fbj6it4t2lv86m",
            10000,
            true,
            Set.of(Txt2ImgCommand.class),
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 7)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "DPM++ SDE Karras")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
                    .put("enable_hr", true)
                    .put("hr_scale", params.aspectRatio.getScaling())
                    .put("hr_upscaler", "R-ESRGAN 4x+ Anime6B")
                    .put("hr_second_pass_steps", 10)
                    .put("denoising_strength", "0.3")
    ),

    KANDINSKY(
            "kandinsky-v2",
            5600,
            false,
            Set.of(Txt2ImgCommand.class),
            params -> new JSONObject()
                    .put("num_steps", 30)
                    .put("guidance_scale", 4)
                    .put("w", (int) (params.aspectRatio.getWidth() * params.aspectRatio.getScaling()))
                    .put("h", (int) (params.aspectRatio.getHeight() * params.aspectRatio.getScaling()))
                    .put("num_images", params.images)
    ),

    PERFECT_WORLD(
            "idpk0o19b3n6ex",
            4800,
            true,
            Set.of(Txt2HentaiCommand.class),
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 5)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
                    .put("enable_hr", true)
                    .put("hr_scale", params.aspectRatio.getScaling())
                    .put("hr_upscaler", "R-ESRGAN 4x+ Anime6B")
                    .put("hr_second_pass_steps", 10)
                    .put("denoising_strength", "0.3")
    ),

    MEINAHENTAI(
            "92mzor4a45vkc1",
            9000,
            true,
            Set.of(Txt2HentaiCommand.class),
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 7)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "DPM++ SDE Karras")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
                    .put("enable_hr", true)
                    .put("hr_scale", params.aspectRatio.getScaling())
                    .put("hr_upscaler", "R-ESRGAN 4x+ Anime6B")
                    .put("hr_second_pass_steps", 10)
                    .put("denoising_strength", "0.3")
    ),

    BB95_FURRY_MIX(
            "7lh2avgf34pryg",
            6900,
            true,
            Set.of(Txt2HentaiCommand.class),
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 10)
                    .put("steps", 50)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("enable_hr", true)
                    .put("hr_scale", params.aspectRatio.getScaling())
                    .put("hr_upscaler", "Latent (nearest)")
                    .put("hr_second_pass_steps", 10)
                    .put("denoising_strength", "0.5")
    );

    private final String modelId;
    private final int expectedTimeMs;
    private final boolean customModel;
    private final Set<Class<? extends RunPodAbstract>> classes;
    private final Function<ModelInputParameters, JSONObject> inputFunction;

    Model(String modelId, int expectedTimeMs, boolean customModel, Set<Class<? extends RunPodAbstract>> classes,
          Function<ModelInputParameters, JSONObject> inputFunction
    ) {
        this.modelId = modelId;
        this.expectedTimeMs = expectedTimeMs;
        this.customModel = customModel;
        this.classes = classes;
        this.inputFunction = inputFunction;
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

    public JSONObject getInput(int images, AspectRatio aspectRatio) {
        return inputFunction.apply(new ModelInputParameters(images, aspectRatio));
    }


    private static class ModelInputParameters {

        private final int images;
        private final AspectRatio aspectRatio;

        public ModelInputParameters(int images, AspectRatio aspectRatio) {
            this.images = images;
            this.aspectRatio = aspectRatio;
        }

    }

}
