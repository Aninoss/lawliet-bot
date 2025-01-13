package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.RunPodAbstract;
import constants.Settings;
import mysql.modules.nsfwfilter.DBNSFWFilters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "txt2hentai",
        emoji = "🖌️",
        executableWithoutArgs = false,
        nsfw = true,
        aliases = {"nsfwimagine", "imaginensfw"}
)
public class Txt2HentaiCommand extends RunPodAbstract {

    private final String[] ADDITIONAL_TAGS = {"chibi"};

    public Txt2HentaiCommand(Locale locale, String prefix) {
        super(locale, prefix, "looks underage, child, ");
    }

    @Override
    public List<String> getFilters(long guildId) {
        List<String> guildFilters = DBNSFWFilters.getInstance().retrieve(guildId).getKeywords();
        ArrayList<String> filters = new ArrayList<>(List.of(Settings.NSFW_FILTERS));
        guildFilters.forEach(filter -> filters.add(filter.toLowerCase()));
        filters.addAll(Arrays.asList(ADDITIONAL_TAGS));
        return filters;
    }

}
