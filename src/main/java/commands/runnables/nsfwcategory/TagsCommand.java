package commands.runnables.nsfwcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.Emojis;
import core.EmbedFactory;
import core.ExceptionLogger;
import modules.porn.BooruTagCache;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "tags",
        emoji = "🏷️",
        executableWithoutArgs = false,
        nsfw = true,
        aliases = {"boorutags", "listtags"}
)
public class TagsCommand extends Command {

    public TagsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        List<String> tags = BooruTagCache.getTags(args);
        if (tags == null) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("invalid", args));
            drawMessage(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        StringBuilder sb = new StringBuilder();
        for (String tag : tags) {
            String line = "- " + tag + "\n";
            if (sb.length() + line.length() > MessageEmbed.VALUE_MAX_LENGTH) {
                eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), sb.toString(), true);
                sb = new StringBuilder();
            }
            sb.append(line);
        }
        if (!sb.isEmpty()) {
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), sb.toString(), true);
        }
        drawMessage(eb).exceptionally(ExceptionLogger.get());
        return true;
    }



}
