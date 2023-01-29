package commands.slashadapters.adapters

import commands.runnables.externalcategory.OsuCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = OsuCommand::class)
class OsuAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val gameMode = generateOptionData(OptionType.STRING, "game_mode", "osu_whichgame").addChoices(
            generateChoice("osu_osu", "osu"),
            generateChoice("osu_taiko", "taiko"),
            generateChoice("osu_fruits", "catch"),
            generateChoice("osu_mania", "mania")
        )
        return commandData
            .addOptions(
                generateOptionData(OptionType.USER, "member", "osu_member", false),
                gameMode
            )
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(OsuCommand::class.java, collectArgs(event))
    }

}