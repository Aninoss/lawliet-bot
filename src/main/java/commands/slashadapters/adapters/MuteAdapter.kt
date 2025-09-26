package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.moderationcategory.MuteCommand
import commands.runnables.moderationcategory.UnmuteCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "mute",
    descriptionCategory = [Category.MODERATION],
    descriptionKey = "mute_description",
    commandAssociations = [MuteCommand::class, UnmuteCommand::class],
)
class MuteAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val muteTrigger = Command.getCommandProperties(MuteCommand::class.java).trigger
        val muteSubcommandData = generateSubcommandData(muteTrigger, "${muteTrigger}_description")
            .addOptions(
                generateOptionData(OptionType.STRING, "members", "moderation_members", true),
                generateOptionData(OptionType.STRING, "reason", "moderation_reason", false),
                generateOptionData(OptionType.STRING, "duration", "moderation_duration", false)
            )

        val unmuteTrigger = Command.getCommandProperties(UnmuteCommand::class.java).trigger
        val unmuteSubcommandData = generateSubcommandData(unmuteTrigger, "${unmuteTrigger}_description")
            .addOptions(
                generateOptionData(OptionType.STRING, "members", "moderation_members", true),
                generateOptionData(OptionType.STRING, "reason", "moderation_reason", false)
            )

        commandData.addSubcommands(muteSubcommandData, unmuteSubcommandData)
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}