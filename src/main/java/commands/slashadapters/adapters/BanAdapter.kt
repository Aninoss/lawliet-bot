package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.moderationcategory.BanCommand
import commands.runnables.moderationcategory.UnbanCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "ban",
    descriptionCategory = [Category.MODERATION],
    descriptionKey = "ban_description",
    commandAssociations = [BanCommand::class, UnbanCommand::class],
)
class BanAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val banTrigger = Command.getCommandProperties(BanCommand::class.java).trigger
        val banSubcommandData = generateSubcommandData(banTrigger, "${banTrigger}_description")
            .addOptions(
                generateOptionData(OptionType.STRING, "members", "moderation_members", true),
                generateOptionData(OptionType.STRING, "reason", "moderation_reason", false),
                generateOptionData(OptionType.STRING, "duration", "moderation_duration", false)
            )

        val unbanTrigger = Command.getCommandProperties(UnbanCommand::class.java).trigger
        val unbanSubcommandData = generateSubcommandData(unbanTrigger, "${unbanTrigger}_description")
            .addOptions(
                generateOptionData(OptionType.STRING, "members", "moderation_members", true),
                generateOptionData(OptionType.STRING, "reason", "moderation_reason", false)
            )

        commandData.addSubcommands(banSubcommandData, unbanSubcommandData)
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}