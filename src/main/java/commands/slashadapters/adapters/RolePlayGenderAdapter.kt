package commands.slashadapters.adapters

import commands.runnables.interactionscategory.RolePlayGenderCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import mysql.hibernate.entity.user.RolePlayGender
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = RolePlayGenderCommand::class)
class RolePlayGenderAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val optionData = generateOptionData(OptionType.STRING, "gender", "rpgender_gender_title", false)
        RolePlayGender.entries.forEach { gender ->
            val choice = generateChoice("rpgender_gender_${gender.id}", gender.id)
            optionData.addChoices(choice)
        }
        return commandData
            .addOptions(optionData)
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(RolePlayGenderCommand::class.java, collectArgs(event))
    }

}