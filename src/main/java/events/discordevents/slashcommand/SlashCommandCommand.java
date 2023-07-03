package events.discordevents.slashcommand;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import commands.Command;
import commands.CommandEvent;
import commands.CommandManager;
import commands.SlashCommandManager;
import commands.slashadapters.SlashMeta;
import core.EmbedFactory;
import core.TextManager;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.SlashCommandAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@DiscordEvent
public class SlashCommandCommand extends SlashCommandAbstract {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public boolean onSlashCommand(SlashCommandInteractionEvent event, EntityManagerWrapper entityManager) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return true;
        }

        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        SlashMeta slashCommandMeta = SlashCommandManager.process(event, guildEntity);
        if (slashCommandMeta == null) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(guildEntity.getLocale(), TextManager.GENERAL, "wrong_args"))
                    .setDescription(TextManager.getString(guildEntity.getLocale(), TextManager.GENERAL, "invalid_noargs"));
            event.replyEmbeds(eb.build())
                    .queue();
            return true;
        }

        String args = slashCommandMeta.getArgs().trim();
        String prefix = guildEntity.getPrefix();
        Locale locale = guildEntity.getLocale();
        Class<? extends Command> clazz = slashCommandMeta.getCommandClass();
        Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
        Function<Locale, String> errorFunction = slashCommandMeta.getErrorFunction();
        if (errorFunction != null) {
            command.getAttachments().put("error", errorFunction.apply(locale));
        }

        deferAfterOneSecond(event);
        CommandEvent commandEvent = new CommandEvent(event);
        try {
            CommandManager.manage(new CommandEvent(event), command, args, guildEntity, getStartTime());
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, commandEvent, guildEntity);
        }

        return true;
    }

    private void deferAfterOneSecond(SlashCommandInteractionEvent event) {
        scheduler.schedule(() -> {
            if (!event.isAcknowledged()) {
                event.deferReply().queue();
            }
        }, 1, TimeUnit.SECONDS);
    }

}
