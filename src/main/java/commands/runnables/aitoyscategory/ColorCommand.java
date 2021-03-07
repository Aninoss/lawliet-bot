package commands.runnables.aitoyscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.DeepAIAbstract;

@CommandProperties(
        trigger = "color",
        withLoadingBar = true,
        emoji = "🎨",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "colourizer", "colorize", "colorizer", "colorization", "colourize", "colour", "colourization" }
)
public class ColorCommand extends DeepAIAbstract {

    public ColorCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getUrl() {
        return "https://api.deepai.org/api/colorizer";
    }

    @Override
    protected DeepAIExample[] getDeepAiExamples() {
        return new DeepAIExample[]{
                new DeepAIExample("https://cdn.britannica.com/18/1918-050-0166D6BB/Martin-Luther-King-Jr.jpg", "https://cdn.discordapp.com/attachments/499629904380297226/772200478632837140/colorizer.jpg"),
                new DeepAIExample("https://upload.wikimedia.org/wikipedia/commons/thumb/2/21/Adams_The_Tetons_and_the_Snake_River.jpg/1280px-Adams_The_Tetons_and_the_Snake_River.jpg", "https://cdn.discordapp.com/attachments/499629904380297226/772201178607910922/colorizer2.jpg")
        };
    }

}
