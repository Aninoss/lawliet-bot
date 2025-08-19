package commands.runnables.moderationcategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.atomicassets.AtomicGuildMessageChannel;
import core.mention.MentionValue;
import core.utils.MentionUtil;
import modules.moderation.ChannelLock;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Locale;

@CommandProperties(
        trigger = "lock",
        botGuildPermissions = {Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.VOICE_CONNECT},
        emoji = "🔒",
        executableWithoutArgs = true
)
public class LockCommand extends Command {

    public LockCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        MentionValue<Long> minuteMention = MentionUtil.getTimeMinutes(args);
        long minutes = minuteMention.getValue();

        GuildMessageChannel channel = ChannelLock.getChannel(this, event, minuteMention.getFilteredArgs());
        if (channel == null) {
            return false;
        }

        GuildEntity guildEntity = getGuildEntity();
        if (guildEntity.getChannelLocks().containsKey(channel.getIdLong())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("error_alreadylocked", new AtomicGuildMessageChannel(channel).getPrefixedNameInField(getLocale())));
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }

        try {
            guildEntity.beginTransaction();
            ChannelLock.lock(guildEntity, channel, event.getChannel().getIdLong(), minutes);
            BotLogEntity.log(guildEntity.getEntityManager(), BotLogEntity.Event.CHANNEL_LOCK, event.getMember(), channel.getIdLong());
            guildEntity.commitTransaction();
        } catch (Throwable e) {
            guildEntity.getEntityManager().getTransaction().rollback();
            throw e;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(
                minutes > 0 ? "success_until" : "success",
                new AtomicGuildMessageChannel(channel).getPrefixedNameInField(getLocale()),
                TimeFormat.DATE_TIME_SHORT.after(Duration.ofMinutes(minutes)).toString()
        ));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
