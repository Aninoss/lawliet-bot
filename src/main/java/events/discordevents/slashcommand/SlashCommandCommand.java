package events.discordevents.slashcommand;

import commands.Command;
import commands.CommandEvent;
import commands.CommandManager;
import commands.SlashCommandManager;
import commands.slashadapters.SlashMeta;
import core.EmbedFactory;
import core.TextManager;
import core.schedule.MainScheduler;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.SlashCommandAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.function.Function;

@DiscordEvent
public class SlashCommandCommand extends SlashCommandAbstract {

    @Override
    public boolean onSlashCommand(SlashCommandInteractionEvent event, EntityManagerWrapper entityManager) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return true;
        }

        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        String prefix = guildEntity.getPrefix();
        Locale locale = guildEntity.getLocale();

        SlashMeta slashCommandMeta = SlashCommandManager.process(event, guildEntity);
        if (slashCommandMeta == null) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(locale, TextManager.GENERAL, "wrong_args"))
                    .setDescription(TextManager.getString(locale, TextManager.GENERAL, "invalid_noargs"));
            event.replyEmbeds(eb.build())
                    .queue();
            return true;
        }

        String args = slashCommandMeta.getArgs().trim();
        Class<? extends Command> clazz = slashCommandMeta.getCommandClass();
        Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
        Function<Locale, String> errorFunction = slashCommandMeta.getErrorFunction();
        if (errorFunction != null) {
            command.getAttachments().put("error", errorFunction.apply(locale));
        }

        deferIfNotAcknowledged(event);
        CommandEvent commandEvent = new CommandEvent(event);
        try {
            CommandManager.manage(new CommandEvent(event), command, args, guildEntity, getStartTime());
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, commandEvent, guildEntity);
        }

        return true;
    }

    private void deferIfNotAcknowledged(SlashCommandInteractionEvent event) {
        int eventAgeMillis = (int) Duration.between(event.getTimeCreated().toInstant(), Instant.now()).toMillis();
        MainScheduler.schedule(Duration.ofMillis(1500 - eventAgeMillis), () -> {
            if (!event.isAcknowledged()) {
                event.deferReply().queue();
            }
        });
    }

}
