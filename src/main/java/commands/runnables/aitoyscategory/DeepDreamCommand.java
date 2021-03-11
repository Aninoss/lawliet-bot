package commands.runnables.aitoyscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.DeepAIAbstract;

@CommandProperties(
        trigger = "deepdream",
        withLoadingBar = true,
        emoji = "💤",
        executableWithoutArgs = true,
        patreonRequired = true,
        aliases = { "dream", "ddream" }
)
public class DeepDreamCommand extends DeepAIAbstract {

    public DeepDreamCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected String getUrl() {
        return "https://api.deepai.org/api/deepdream";
    }

    @Override
    protected DeepAIExample[] getDeepAiExamples() {
        return new DeepAIExample[] {
                new DeepAIExample("https://naturpark-schaffhausen.ch/assets/images/a/Opfertshofen_Abendsonne_Landschaft-fee55e64.webp", "https://cdn.discordapp.com/attachments/499629904380297226/800356139916066836/deepdream.jpg"),
                new DeepAIExample("https://images.pexels.com/photos/73871/rocket-launch-rocket-take-off-nasa-73871.jpeg", "https://cdn.discordapp.com/attachments/499629904380297226/800359090911576084/deepdream2.jpg")
        };
    }

}
