package commands.slashadapters.adapters

import commands.runnables.utilitycategory.VoteCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = VoteCommand::class)
class VoteAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "topic", "vote_slash_topic", true),
            generateOptionData(OptionType.STRING, "answer1", "vote_slash_answer1", true),
            generateOptionData(OptionType.STRING, "answer2", "vote_slash_answer2", true),
            generateOptionData(OptionType.STRING, "answer3", "vote_slash_answer3", false),
            generateOptionData(OptionType.STRING, "answer4", "vote_slash_answer4", false),
            generateOptionData(OptionType.STRING, "answer5", "vote_slash_answer5", false),
            generateOptionData(OptionType.STRING, "answer6", "vote_slash_answer6", false),
            generateOptionData(OptionType.STRING, "answer7", "vote_slash_answer7", false),
            generateOptionData(OptionType.STRING, "answer8", "vote_slash_answer8", false),
            generateOptionData(OptionType.STRING, "answer9", "vote_slash_answer9", false),
            generateOptionData(OptionType.CHANNEL, "channel", "vote_slash_channel", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val argsBuilder = StringBuilder()
        for (option in event.options) {
            if (argsBuilder.length > 0) {
                if (option.type == OptionType.STRING) {
                    argsBuilder.append("|")
                } else {
                    argsBuilder.append(" ")
                }
            }
            argsBuilder.append(option.asString.replace("|", "\\|"))
        }
        return SlashMeta(VoteCommand::class.java, argsBuilder.toString())
    }

}