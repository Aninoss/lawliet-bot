package commands.runnables.informationcategory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.ShardManager;
import core.utils.BotUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.modules.survey.DBSurvey;
import mysql.modules.version.DBVersion;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "stats",
        emoji = "\uD83D\uDCCA",
        executableWithoutArgs = true,
        onlyPublicVersion = true,
        aliases = { "info" }
)
public class StatsCommand extends Command {

    public StatsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        String dephordName = "???";
        try {
            dephordName = ShardManager.getInstance().fetchUserById(303085910784737281L).get().getAsTag();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
        }

        String neverCookFirstName = "???";
        try {
            neverCookFirstName = ShardManager.getInstance().fetchUserById(298153126223937538L).get().getAsTag();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
        }

        String owner = ShardManager.getInstance().fetchOwner().get().getAsTag();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString(
                        "template",
                        owner,
                        ExternalLinks.BOT_INVITE_URL,
                        BotUtil.getCurrentVersion(),
                        TimeUtil.getInstantString(getLocale(), DBVersion.getInstance().retrieve().getCurrentVersion().getDate(), true),
                        ShardManager.getInstance().getGlobalGuildSize().map(StringUtil::numToString).orElse("-"),
                        owner,
                        StringUtil.numToString(DBSurvey.getInstance().getCurrentSurvey().getFirstVoteNumber())
                ) + "\n\n" + getString("translator", dephordName, neverCookFirstName)
        );

        event.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

}
