package commands.runnables.aitoyscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RunPodAbstract;

import java.util.Locale;

@CommandProperties(
        trigger = "txt2img",
        emoji = "🖌️",
        executableWithoutArgs = false,
        patreonRequired = true,
        aliases = {"stablediffusion", "diffusion", "imagine"}
)
public class Txt2ImgCommand extends RunPodAbstract {

    private static final String[] CONTENT_FILTERS = {"nsfw", "porn", "porno", "pornography", "sex", "intercourse", "coitus",
            "explicit", "fuck", "fucking", "fucked", "rape", "raping", "raped", "blowjob", "anal", "naked", "penis",
            "cock", "dick", "vagina", "pussy", "cum", "sperm", "horny", "scat"};

    public Txt2ImgCommand(Locale locale, String prefix) {
        super(locale, prefix, CONTENT_FILTERS);
    }

}
