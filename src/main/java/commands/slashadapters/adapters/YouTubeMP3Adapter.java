package commands.slashadapters.adapters;

import commands.runnables.externalcategory.YouTubeMP3Command;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Slash(command = YouTubeMP3Command.class)
public class YouTubeMP3Adapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        return commandData
                .addOption(OptionType.STRING, "video_url", "A link to the YouTube video", true);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(YouTubeMP3Command.class, collectArgs(event));
    }

}
