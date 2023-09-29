package commands.runnables.utilitycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.configurationcategory.CustomConfigCommand;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.cache.ServerPatreonBoostCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.CustomCommandEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.SortedMap;

@CommandProperties(
        trigger = "custom",
        emoji = "🧩",
        executableWithoutArgs = false
)
public class CustomCommand extends Command {

    public CustomCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        SortedMap<String, CustomCommandEntity> customCommands = getGuildEntity().getCustomCommands();
        CustomCommandEntity customCommand = customCommands.get(args);
        if (customCommand == null) {
            EmbedBuilder eb = EmbedFactory.getNoResultsEmbed(this, args);
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        if (customCommands.size() > CustomConfigCommand.MAX_COMMANDS_FREE) {
            if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                FeatureLogger.inc(PremiumFeature.CUSTOM_COMMANDS, event.getGuild().getIdLong());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("nopro"));
                drawMessageNew(eb).exceptionally(ExceptionLogger.get());
                return false;
            }
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, StringUtil.shortenString(customCommand.getTextResponse(), MessageEmbed.VALUE_MAX_LENGTH))
                .setTitle(null);
        EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "serverstaff_text"));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
