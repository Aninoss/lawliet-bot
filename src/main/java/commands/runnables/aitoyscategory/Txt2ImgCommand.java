package commands.runnables.aitoyscategory;

import commands.listeners.CommandProperties;
import commands.runnables.RunPodAbstract;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "txt2img",
        emoji = "🖌️",
        executableWithoutArgs = false,
        aliases = {"stablediffusion", "diffusion", "imagine"}
)
public class Txt2ImgCommand extends RunPodAbstract {

    private static final String[] NSFW_CONTENT_FILTERS = {
            "nsfw", "porn", "porno", "pornography", "sex", "sexual", "intercourse", "coitus", "dicked", "masturbate",
            "masturbating", "masturbation", "explicit", "fuck", "fucking", "fucked", "blowjob", "hentai", "bdsm", "anal",
            "ass", "nude", "naked", "penis", "cock", "dick", "vagina", "pussy", "cum", "sperm", "horny", "scat", "lewd",
            "nipple", "nipples", "topless", "dildo", "vore", "boobs"
    };

    public Txt2ImgCommand(Locale locale, String prefix) {
        super(locale, prefix, "nipples, naked, topless, ");
    }

    @Override
    public List<String> getFilters(long guildId) {
        return List.of(NSFW_CONTENT_FILTERS);
    }

}
