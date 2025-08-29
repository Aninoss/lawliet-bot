package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.atomicassets.AtomicGuildMessageChannel;
import modules.moderation.ChannelLock;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandProperties(
        trigger = "unlock",
        botGuildPermissions = {Permission.MANAGE_PERMISSIONS},
        userGuildPermissions = {Permission.MANAGE_PERMISSIONS},
        emoji = "🔒",
        executableWithoutArgs = true
)
public class UnlockCommand extends LockCommand {

    public UnlockCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        GuildMessageChannel channel = ChannelLock.getChannel(this, event, args);
        if (channel == null) {
            return false;
        }

        GuildEntity guildEntity = getGuildEntity();
        if (!guildEntity.getChannelLocks().containsKey(channel.getIdLong())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("error_notlocked", new AtomicGuildMessageChannel(channel).getPrefixedNameInField(getLocale())));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        try {
            guildEntity.beginTransaction();
            ChannelLock.unlock(guildEntity, channel);
            BotLogEntity.log(guildEntity.getEntityManager(), BotLogEntity.Event.CHANNEL_UNLOCK, event.getMember(), channel.getIdLong());
            guildEntity.commitTransaction();
        } catch (Throwable e) {
            guildEntity.getEntityManager().getTransaction().rollback();
            throw e;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("success", new AtomicGuildMessageChannel(channel).getPrefixedNameInField(getLocale())));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
