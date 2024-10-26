package modules.txt2img;

import commands.runnables.RunPodAbstract;
import commands.runnables.aitoyscategory.Txt2ImgCommand;
import commands.runnables.nsfwcategory.Txt2HentaiCommand;
import org.json.JSONObject;

import java.util.Set;
import java.util.function.Function;

public enum StableDiffusionModel {

    REALISTIC_VISION(
            "f3xzap2iaijkd2",
            9200,
            true,
            true,
            Set.of(Txt2ImgCommand.class),
            "",
            "",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth(false))
                    .put("height", params.aspectRatio.getHeight(false))
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
            true,
            Set.of(Txt2ImgCommand.class),
            "",
            "",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth(false))
                    .put("height", params.aspectRatio.getHeight(false))
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
            true,
            Set.of(Txt2ImgCommand.class),
            "",
            "",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth(false))
                    .put("height", params.aspectRatio.getHeight(false))
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
            true,
            Set.of(Txt2ImgCommand.class),
            "",
            "",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth(false))
                    .put("height", params.aspectRatio.getHeight(false))
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

    WAI_ANI(
            "sf4z2vuf6zo417",
            12000,
            true,
            false,
            Set.of(Txt2HentaiCommand.class),
            ", source_anime, score_9, score_8_up, score_7_up, zPDXL3, zPDXLxxx, rating_explicit",
            ", 3d",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth(true))
                    .put("height", params.aspectRatio.getHeight(true))
                    .put("cfg_scale", 7)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    ),

    SEMIMERGEIJ(
            "idpk0o19b3n6ex",
            12000,
            true,
            false,
            Set.of(Txt2HentaiCommand.class),
            ", source_anime, score_9, score_8_up, score_7_up, zPDXL3, zPDXLxxx, rating_explicit",
            "",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth(true))
                    .put("height", params.aspectRatio.getHeight(true))
                    .put("cfg_scale", 7)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    ),

    NOVA_FURRY(
            "7lh2avgf34pryg",
            12000,
            true,
            false,
            Set.of(Txt2HentaiCommand.class),
            ", source_furry, BREAK, furry, anthro, detailed face and eyes, zPDXL3, zPDXLxxx, rating_explicit",
            "",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth(true))
                    .put("height", params.aspectRatio.getHeight(true))
                    .put("cfg_scale", 7)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    ),

    NOVA_ANIMAL(
            "h0agh6vvqgp0bv",
            12000,
            true,
            false,
            Set.of(Txt2HentaiCommand.class),
            ", source_furry, BREAK, furry, anthro, realistic, photo, detailed face and eyes, zPDXL3, zPDXLxxx, rating_explicit",
            "",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth(true))
                    .put("height", params.aspectRatio.getHeight(true))
                    .put("cfg_scale", 7)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    );

    private final String modelId;
    private final int expectedTimeMs;
    private final boolean customModel;
    private final boolean checkNsfw;
    private final Set<Class<? extends RunPodAbstract>> classes;
    private final String additionalPrompt;
    private final String additionalNegativePrompt;
    private final Function<ModelInputParameters, JSONObject> inputFunction;

    StableDiffusionModel(String modelId, int expectedTimeMs, boolean customModel, boolean checkNsfw, Set<Class<? extends RunPodAbstract>> classes,
                         String additionalPrompt, String additionalNegativePrompt, Function<ModelInputParameters, JSONObject> inputFunction
    ) {
        this.modelId = modelId;
        this.expectedTimeMs = expectedTimeMs;
        this.customModel = customModel;
        this.checkNsfw = checkNsfw;
        this.classes = classes;
        this.additionalPrompt = additionalPrompt;
        this.additionalNegativePrompt = additionalNegativePrompt;
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

    public boolean getCheckNsfw() {
        return checkNsfw;
    }

    public Set<Class<? extends RunPodAbstract>> getClasses() {
        return classes;
    }

    public String getAdditionalPrompt() {
        return additionalPrompt;
    }

    public String getAdditionalNegativePrompt() {
        return additionalNegativePrompt;
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
