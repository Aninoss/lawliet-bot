package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.AssetIds;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.Program;
import core.ShardManager;
import core.utils.BotUtil;
import core.utils.StringUtil;
import mysql.modules.version.DBVersion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "stats",
        emoji = "\uD83D\uDCCA",
        executableWithoutArgs = true,
        aliases = { "info" }
)
public class StatsCommand extends Command {

    public StatsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        deferReply();

        String dephordName = "???";
        try {
            dephordName = StringUtil.escapeMarkdown(ShardManager.fetchUserById(303085910784737281L).get().getName());
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
        }

        String neverCookFirstName = "???";
        try {
            neverCookFirstName = StringUtil.escapeMarkdown(ShardManager.fetchUserById(298153126223937538L).get().getName());
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
        }

        String laleName = "???";
        try {
            laleName = StringUtil.escapeMarkdown(ShardManager.fetchUserById(774017093309431808L).get().getName());
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
        }

        String owner = StringUtil.escapeMarkdown(ShardManager.fetchUserById(AssetIds.OWNER_USER_ID).get().getName());
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString(
                        "template",
                        owner,
                        "",
                        BotUtil.getCurrentVersion(),
                        TimeFormat.DATE_TIME_SHORT.atInstant(DBVersion.getInstance().retrieve().getCurrentVersion().getDate()).toString(),
                        ShardManager.getGlobalGuildSize().map(StringUtil::numToString).orElse("-"),
                        owner,
                        StringUtil.numToString(event.getJDA().getShardInfo().getShardId()),
                        StringUtil.numToString(Program.publicInstance() ? Program.getClusterId() : 1)
                ) + "\n\n" + getString("translator", dephordName, neverCookFirstName, laleName)
        );

        if (Program.publicInstance()) {
            setComponents(Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_URL, getString("invite")));
        }
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
