package commands.slashadapters.adapters;

import commands.runnables.gimmickscategory.QuoteCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = QuoteCommand.class)
public class QuoteAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.CHANNEL, "channel", "The text channel of the message", false)
                .addOption(OptionType.STRING, "message_id", "The id of the message", false)
                .addOption(OptionType.STRING, "message_link", "The link of the message", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(QuoteCommand.class, collectArgs(event));
    }

}
