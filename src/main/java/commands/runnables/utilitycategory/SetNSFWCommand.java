package commands.runnables.utilitycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.atomicassets.AtomicGuildChannel;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.managers.channel.ChannelManager;
import net.dv8tion.jda.api.managers.channel.attribute.IAgeRestrictedChannelManager;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@CommandProperties(
        trigger = "setnsfw",
        botChannelPermissions = Permission.MANAGE_CHANNEL,
        userChannelPermissions = Permission.MANAGE_CHANNEL,
        emoji = "🔞",
        executableWithoutArgs = true
)
public class SetNSFWCommand extends Command {

    public SetNSFWCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        MentionList<GuildMessageChannel> channelMention = MentionUtil.getGuildMessageChannels(event.getGuild(), args);
        GuildMessageChannel targetChannel = event.getMessageChannel();
        if (!channelMention.getList().isEmpty()) {
            targetChannel = channelMention.getList().get(0);
        }

        GuildChannel channel = getRootChannel(targetChannel);
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionsMissingEmbed(
                getLocale(),
                channel,
                event.getMember(),
                new Permission[0],
                new Permission[] { Permission.MANAGE_CHANNEL },
                new Permission[0],
                new Permission[] { Permission.MANAGE_CHANNEL }
        );
        if (errEmbed != null) {
            drawMessageNew(errEmbed).exceptionally(ExceptionLogger.get());
            return false;
        }

        boolean nsfwTarget = !JDAUtil.channelIsNsfw(channel);
        switchAgeRestricted(channel, nsfwTarget);

        EntityManagerWrapper entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        BotLogEntity.log(entityManager, nsfwTarget ? BotLogEntity.Event.SET_NSFW : BotLogEntity.Event.SET_NOT_NSFW, event.getMember(), channel.getIdLong());
        entityManager.getTransaction().commit();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(nsfwTarget ? "setnsfw" : "setnotnsfw", new AtomicGuildChannel(channel).getAsMention(getLocale())));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

    private GuildChannel getRootChannel(GuildMessageChannel channel) {
        if (channel instanceof ThreadChannel) {
            return ((ThreadChannel) channel).getParentChannel();
        }
        return channel;
    }

    private void switchAgeRestricted(GuildChannel channel, boolean nsfwTarget) {
        ChannelManager<?, ?> channelManager = channel.getManager();
        if (channelManager instanceof IAgeRestrictedChannelManager) {
            ((IAgeRestrictedChannelManager<?, ?>) channelManager)
                    .setNSFW(nsfwTarget)
                    .reason(getCommandLanguage().getTitle())
                    .complete();
        }
    }

}
