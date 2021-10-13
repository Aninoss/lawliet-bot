package events.discordevents.slashcommand;

import java.util.Locale;
import commands.Command;
import commands.CommandContainer;
import commands.CommandEvent;
import commands.CommandManager;
import commands.runnables.informationcategory.HelpCommand;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.SlashCommandAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

@DiscordEvent
public class SlashCommandCommand extends SlashCommandAbstract {

    @Override
    public boolean onSlashCommand(SlashCommandEvent event) throws Throwable {
        StringBuilder argsBuilder = new StringBuilder();
        for (OptionMapping option : event.getOptions()) {
            argsBuilder.append(option.getAsString()).append(" ");
        }
        String args = argsBuilder.toString();

        GuildData guildData = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        String prefix = guildData.getPrefix();
        Locale locale = guildData.getLocale();
        Class<? extends Command> clazz = CommandContainer.getCommandMap().get(event.getName());
        if (clazz != null) {
            Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
            if (!command.getCommandProperties().executableWithoutArgs() && args.isEmpty()) {
                args = command.getTrigger();
                command = CommandManager.createCommandByClass(HelpCommand.class, locale, prefix);
                command.getAttachments().put("noargs", true);
            }

            try {
                CommandManager.manage(new CommandEvent(event), command, args, getStartTime());
            } catch (Throwable e) {
                ExceptionUtil.handleCommandException(e, command);
            }
        }

        return true;
    }

}
