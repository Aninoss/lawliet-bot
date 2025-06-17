package modules.txt2img;

import commands.runnables.RunPodAbstract;
import commands.runnables.aitoyscategory.Txt2ImgCommand;
import commands.runnables.nsfwcategory.Txt2HentaiCommand;
import org.json.JSONObject;

import java.util.Set;
import java.util.function.Function;

public enum StableDiffusionModel {

    RILLUSM(
            "gevo8ndzux6u9i",
            12000,
            false,
            Set.of(Txt2ImgCommand.class, Txt2HentaiCommand.class),
            "extremely detailed, masterpiece, best quality, amazing quality, photorealistic, ",
            "bad face quality, undetailed face, blurry, bad anatomy, bad fingers, bad hands, bad quality, worst quality, worst detail, (digital art, cgi, 3d, render, drawing, graphics, anime, cartoon), ",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 5)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 1))
    ),

    ILUSTMIX(
            "idpk0o19b3n6ex",
            12000,
            false,
            Set.of(Txt2ImgCommand.class, Txt2HentaiCommand.class),
            "masterpiece, best quality, amazing quality, very aesthetic, detailed eyes, perfect eyes, realistic eyes, ",
            "bad quality, worst quality, worst detail, sketch, censor, signature, patreon, watermark, artist name, cropped, ",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 7)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    ),

    NOVA_ANIME(
            "qxq0ff5d0begby",
            12000,
            false,
            Set.of(Txt2ImgCommand.class),
            "masterpiece, best quality, amazing quality, ",
            "jpeg artifacts, username, watermark, signature, normal quality, worst quality, large head, low quality, text, error, missing fingers, extra digits, fewer digits, bad eye, ",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 6)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    ),

    WAI_NSFW(
            "p5ocrvhvqi5k3w",
            12000,
            false,
            Set.of(Txt2HentaiCommand.class),
            "masterpiece, best quality, amazing quality, ",
            "bad quality, worst quality, worst detail, sketch, censor, signature, patreon, watermark, artist name, cropped, ",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 6)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    ),

    NOVA_3DCG(
            "yq3klb4sx1f1xn",
            12000,
            false,
            Set.of(Txt2ImgCommand.class, Txt2HentaiCommand.class),
            "masterpiece, best quality, amazing quality, very aesthetic, high resolution, ultra-detailed, absurdres, newest, scenery, 3D, rendered, BREAK, depth of field, volumetric lighting, lazyreal, ",
            "modern, recent, old, oldest, cartoon, graphic, text, painting, crayon, graphite, abstract, glitch, deformed, mutated, ugly, disfigured, long body, lowres, bad anatomy, bad hands, missing fingers, extra digits, fewer digits, cropped, very displeasing, (worst quality, bad quality:1.2), bad anatomy, sketch, jpeg artifacts, signature, watermark, username, simple background, conjoined, bad ai-generated, ",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 5)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    ),

    NOVA_FURRY(
            "c3skif2r7lkbyu",
            12000,
            false,
            Set.of(Txt2ImgCommand.class, Txt2HentaiCommand.class),
            "masterpiece, best quality, amazing quality, very aesthetic, high resolution, ultra-detailed, absurdres, newest, scenery, furry, anthro, BREAK, detailed background, detailed hair, depth of field, detailed fluffy fur, photorealistic details, volumetric lighting, ",
            "human, multiple tails, modern, recent, old, oldest, graphic, cartoon, text, painting, crayon, graphite, abstract, glitch, deformed, mutated, ugly, disfigured, long body, lowres, bad anatomy, bad hands, missing fingers, extra digits, fewer digits, cropped, very displeasing, (worst quality, bad quality:1.2), bad anatomy, sketch, jpeg artifacts, signature, watermark, username, simple background, conjoined, bad ai-generated, ",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 4)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    ),

    NOVA_ANIMAL(
            "uer5tf6bt0fe35",
            12000,
            false,
            Set.of(Txt2ImgCommand.class, Txt2HentaiCommand.class),
            "masterpiece, best quality, amazing quality, very aesthetic, high resolution, ultra-detailed, absurdres, scenery, photo, realistic, furry, anthro, BREAK, depth of field, detailed fluffy fur, photorealistic details, ",
            "smooth skin, human, multiple tails, 2d, art, old, oldest, cartoon, graphic, text, painting, crayon, graphite, abstract, glitch, deformed, mutated, ugly, disfigured, long body, lowres, bad anatomy, bad hands, missing fingers, extra digits, fewer digits, very displeasing, (worst quality, bad quality:1.2), bad anatomy, sketch, jpeg artifacts, signature, watermark, username, simple background, conjoined, bad ai-generated, ",
            params -> new JSONObject()
                    .put("width", params.aspectRatio.getWidth())
                    .put("height", params.aspectRatio.getHeight())
                    .put("cfg_scale", 5)
                    .put("steps", 30)
                    .put("batch_size", params.images)
                    .put("sampler_name", "Euler a")
                    .put("override_settings", new JSONObject().put("CLIP_stop_at_last_layers", 2))
    );

    private final String modelId;
    private final int expectedTimeMs;
    private final boolean restoreFaces;
    private final Set<Class<? extends RunPodAbstract>> classes;
    private final String additionalPrompt;
    private final String additionalNegativePrompt;
    private final Function<ModelInputParameters, JSONObject> inputFunction;

    StableDiffusionModel(String modelId, int expectedTimeMs, boolean restoreFaces, Set<Class<? extends RunPodAbstract>> classes,
                         String additionalPrompt, String additionalNegativePrompt,
                         Function<ModelInputParameters, JSONObject> inputFunction
    ) {
        this.modelId = modelId;
        this.expectedTimeMs = expectedTimeMs;
        this.restoreFaces = restoreFaces;
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

    public boolean getRestoreFaces() {
        return restoreFaces;
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
