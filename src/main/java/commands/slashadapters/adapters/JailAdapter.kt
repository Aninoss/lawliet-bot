package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.moderationcategory.JailCommand
import commands.runnables.moderationcategory.UnjailCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "jail",
    descriptionCategory = [Category.MODERATION],
    descriptionKey = "jail_description",
    commandAssociations = [JailCommand::class, UnjailCommand::class],
)
class JailAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val jailTrigger = Command.getCommandProperties(JailCommand::class.java).trigger
        val jailSubcommandData = generateSubcommandData(jailTrigger, "${jailTrigger}_description")
            .addOptions(
                generateOptionData(OptionType.STRING, "members", "moderation_members", true),
                generateOptionData(OptionType.STRING, "reason", "moderation_reason", false),
                generateOptionData(OptionType.STRING, "duration", "moderation_duration", false)
            )

        val unjailTrigger = Command.getCommandProperties(UnjailCommand::class.java).trigger
        val unjailSubcommandData = generateSubcommandData(unjailTrigger, "${unjailTrigger}_description")
            .addOptions(
                generateOptionData(OptionType.STRING, "members", "moderation_members", true),
                generateOptionData(OptionType.STRING, "reason", "moderation_reason", false)
            )

        commandData.addSubcommands(jailSubcommandData, unjailSubcommandData)
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}