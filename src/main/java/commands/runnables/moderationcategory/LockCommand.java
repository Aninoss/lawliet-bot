package commands.runnables.moderationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.atomicassets.AtomicGuildMessageChannel;
import core.utils.CommandUtil;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandProperties(
        trigger = "lock",
        emoji = "🔒",
        executableWithoutArgs = true
)
public class LockCommand extends Command {

    public LockCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        GuildMessageChannel channel;
        CommandUtil.ChannelResponse response = CommandUtil.differentChannelExtract(this, event, event.getMessageChannel(), args, Permission.MANAGE_PERMISSIONS);
        if (response != null) {
            channel = response.getChannel();
        } else {
            return false;
        }

        GuildEntity guildEntity = getGuildEntity();
        if (guildEntity.getChannelLocks().containsKey(channel.getIdLong())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("error_alreadylocked", new AtomicGuildMessageChannel(channel).getPrefixedNameInField(getLocale())));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        return true;
    }

}
