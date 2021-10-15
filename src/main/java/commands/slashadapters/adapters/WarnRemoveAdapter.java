package commands.slashadapters.adapters;

import commands.runnables.moderationcategory.WarnRemoveCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = WarnRemoveCommand.class)
public class WarnRemoveAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.USER, "member", "The user to lose a specific number of warns", false)
                .addOption(OptionType.USER, "member2", "The user to lose a specific number of warns", false)
                .addOption(OptionType.USER, "member3", "The user to lose a specific number of warns", false)
                .addOption(OptionType.USER, "member4", "The user to lose a specific number of warns", false)
                .addOption(OptionType.USER, "member5", "The user to lose a specific number of warns", false)
                .addOption(OptionType.STRING, "member_id", "The user to lose a specific number of warns", false)
                .addOption(OptionType.STRING, "reason", "The reason of this mod action", false)
                .addOption(OptionType.NUMBER, "amount", "How many warns shall be removed?", false)
                .addOption(OptionType.BOOLEAN, "all", "Remove all warns", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        String args;
        if (event.getOption("all") != null && event.getOption("all").getAsBoolean()) {
            args = collectArgs(event, "amount");
        } else {
            args = collectArgs(event);
        }

        return new SlashMeta(WarnRemoveCommand.class, args);
    }

}
