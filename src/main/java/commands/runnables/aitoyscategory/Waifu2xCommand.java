package commands.runnables.aitoyscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.DeepAIAbstract;

@CommandProperties(
        trigger = "waifu2x",
        withLoadingBar = true,
        emoji = "\uD83D\uDCC8",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "waifu4x" }
)
public class Waifu2xCommand extends DeepAIAbstract {

    public Waifu2xCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getUrl() {
        return "https://api.deepai.org/api/waifu2x";
    }

    @Override
    protected DeepAIExample[] getDeepAiExamples() {
        return new DeepAIExample[]{
                new DeepAIExample("https://i.pinimg.com/236x/a4/a6/43/a4a6430b557982c69b50bcf174c6077f.jpg", "https://cdn.discordapp.com/attachments/499629904380297226/611959216038477825/waifu2x.jpg"),
                new DeepAIExample("https://avatarfiles.alphacoders.com/699/thumb-69905.png", "https://cdn.discordapp.com/attachments/499629904380297226/611960284239626241/waifu2x2.jpg")
        };
    }

}
