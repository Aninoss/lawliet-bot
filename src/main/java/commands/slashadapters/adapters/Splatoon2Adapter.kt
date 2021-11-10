package commands.slashadapters.adapters;

import commands.Category;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import constants.Language;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

@Slash(name = "splatoon2", description = "View current data about Splatoon 2")
public class Splatoon2Adapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        for (Class<? extends Command> clazz : CommandContainer.getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, Language.EN.getLocale(), "/");
            if (command.getCategory() == Category.SPLATOON_2) {
                SubcommandData subcommandData = new SubcommandData(command.getCommandProperties().trigger(), command.getCommandLanguage().getDescShort());
                commandData.addSubcommands(subcommandData);
            }
        }

        return commandData;
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        String trigger = event.getSubcommandName();
        Class<? extends Command> clazz = CommandContainer.getCommandMap().get(trigger);
        return new SlashMeta(clazz, "");
    }

}
