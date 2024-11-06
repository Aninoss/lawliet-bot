package events.discordevents.guildmessagecontextinteraction;

import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.CommandManager;
import commands.runnables.aitoyscategory.TranslateCommand;
import constants.Language;
import core.TextManager;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMessageContextInteractionAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;

import java.time.Instant;
import java.util.Arrays;

@DiscordEvent
public class GuildMessageContextInteractionTranslate extends GuildMessageContextInteractionAbstract {

    @Override
    public boolean onGuildMessageContextInteraction(MessageContextInteractionEvent event, EntityManagerWrapper entityManager) {
        if (Arrays.stream(Language.values())
                .noneMatch(language -> TextManager.getString(language.getLocale(), Category.AI_TOYS, "translate_interaction").equals(event.getCommandString()))
        ) {
            return true;
        }

        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        Command command = CommandManager.createCommandByClass(TranslateCommand.class, guildEntity.getLocale(), guildEntity.getPrefix());
        CommandEvent commandEvent = new CommandEvent(event);
        try {
            command.setEphemeralMessages(true);
            CommandManager.manage(commandEvent, command, event.getTarget().getJumpUrl(), guildEntity, Instant.now());
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command, commandEvent, guildEntity);
        }

        return true;
    }

}
