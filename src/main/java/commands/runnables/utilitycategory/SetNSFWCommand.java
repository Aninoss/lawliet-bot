package commands.runnables.utilitycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.atomicassets.AtomicTextChannel;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
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
        TextChannel channel = event.getTextChannel();
        channel.getManager()
                .setNSFW(!channel.isNSFW())
                .reason(getCommandLanguage().getTitle())
                .complete();

        EntityManagerWrapper entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        BotLogEntity.log(entityManager, channel.isNSFW() ? BotLogEntity.Event.SET_NSFW : BotLogEntity.Event.SET_NOT_NSFW, event.getMember(), channel.getIdLong());
        entityManager.getTransaction().commit();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString(channel.isNSFW() ? "setnsfw" : "setnotnsfw", new AtomicTextChannel(channel).getPrefixedNameInField(getLocale())));
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
        return true;
    }

}
