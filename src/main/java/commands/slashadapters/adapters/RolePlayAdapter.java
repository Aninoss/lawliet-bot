package commands.slashadapters.adapters;

import commands.Category;
import commands.Command;
import commands.CommandContainer;
import commands.runnables.informationcategory.HelpCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import core.TextManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(name = "rp", description = "Interact with other server members")
public class RolePlayAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "gesture", "Which type of interaction? (e.g. hug, kiss)", true)
                .addOption(OptionType.USER, "member", "Who should be involved?", false)
                .addOption(OptionType.USER, "member2", "Who should be involved?", false)
                .addOption(OptionType.USER, "member3", "Who should be involved?", false)
                .addOption(OptionType.USER, "member4", "Who should be involved?", false)
                .addOption(OptionType.USER, "member5", "Who should be involved?", false)
                .addOption(OptionType.BOOLEAN, "everyone", "If you want to mention everyone", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        String type = event.getOption("gesture").getAsString();
        Class<? extends Command> clazz = CommandContainer.getCommandMap().get(type);
        if (clazz != null) {
            if (Command.getCategory(clazz) == Category.INTERACTIONS) {
                return new SlashMeta(clazz, collectArgs(event, "gesture"));
            }
        }
        return new SlashMeta(HelpCommand.class, "interactions", locale -> TextManager.getString(locale, TextManager.COMMANDS, "slash_error_invalidgesture", type));
    }

}
