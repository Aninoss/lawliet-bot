package commands.slashadapters.adapters;

import commands.runnables.externalcategory.TwitchCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = TwitchCommand.class)
public class TwitchAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "twitch_channel_name", "The name of the twitch channel", true);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(TwitchCommand.class, collectArgs(event));
    }

}
