package commands.runnables.nsfwcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.RunPodAbstract;
import constants.Settings;
import mysql.modules.nsfwfilter.DBNSFWFilters;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@CommandProperties(
        trigger = "txt2hentai",
        emoji = "🖌️",
        executableWithoutArgs = false,
        patreonRequired = true,
        nsfw = true,
        aliases = {"nsfwimagine", "imaginensfw"}
)
public class Txt2HentaiCommand extends RunPodAbstract {

    public Txt2HentaiCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public List<String> getFilters(long guildId) {
        List<String> guildFilters = DBNSFWFilters.getInstance().retrieve(guildId).getKeywords();
        ArrayList<String> filters = new ArrayList<>(List.of(Settings.NSFW_FILTERS));
        guildFilters.forEach(filter -> filters.add(filter.toLowerCase()));
        return filters;
    }

}
