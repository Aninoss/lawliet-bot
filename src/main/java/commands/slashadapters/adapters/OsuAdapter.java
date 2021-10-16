package commands.slashadapters.adapters;

import commands.runnables.externalcategory.OsuCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Slash(command = OsuCommand.class)
public class OsuAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        OptionData gameMode = new OptionData(OptionType.STRING, "game_mode", "Which game mode to you want to view?")
                .addChoice("osu!", "osu")
                .addChoice("osu!taiko", "taiko")
                .addChoice("osu!catch", "catch")
                .addChoice("osu!mania", "mania");

        return commandData
                .addOption(OptionType.USER, "member", "Request for another server member", false)
                .addOptions(gameMode);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        return new SlashMeta(OsuCommand.class, collectArgs(event));
    }

}
