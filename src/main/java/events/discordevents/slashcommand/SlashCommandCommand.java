package events.discordevents.slashcommand;

import java.util.Locale;
import java.util.function.Function;
import commands.Command;
import commands.CommandContainer;
import commands.CommandEvent;
import commands.CommandManager;
import commands.SlashCommandManager;
import commands.slashadapters.SlashMeta;
import core.EmbedFactory;
import core.TextManager;
import core.utils.ExceptionUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.SlashCommandAbstract;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;

@DiscordEvent
public class SlashCommandCommand extends SlashCommandAbstract {

    @Override
    public boolean onSlashCommand(SlashCommandEvent event) throws Throwable {
        SlashMeta slashCommandMeta = SlashCommandManager.process(event);
        if (slashCommandMeta == null) {
            GuildData guildData = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(guildData.getLocale(), TextManager.GENERAL, "wrong_args"))
                    .setDescription(TextManager.getString(guildData.getLocale(), TextManager.GENERAL, "invalid_noargs"));
            event.getHook().sendMessageEmbeds(eb.build())
                    .queue();
            return true;
        }

        GuildData guildData = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        String trigger = slashCommandMeta.getTrigger();
        String args = slashCommandMeta.getArgs();
        String prefix = guildData.getPrefix();
        Locale locale = guildData.getLocale();
        Class<? extends Command> clazz = CommandContainer.getCommandMap().get(trigger);
        if (clazz != null) {
            Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
            Function<Locale, String> errorFunction = slashCommandMeta.getErrorFunction();
            if (errorFunction != null) {
                command.getAttachments().put("error", errorFunction.apply(locale));
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
