package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.moderationcategory.LockCommand
import commands.runnables.moderationcategory.UnlockCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "channellock",
    descriptionCategory = [Category.MODERATION],
    descriptionKey = "lock_description",
    commandAssociations = [LockCommand::class, UnlockCommand::class],
)
class ChannelLockAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val lockTrigger = Command.getCommandProperties(LockCommand::class.java).trigger
        val lockSubcommandData = generateSubcommandData(lockTrigger, "${lockTrigger}_description")
            .addOptions(
                generateOptionData(OptionType.CHANNEL, "channel", "lock_slash_channel", false),
                generateOptionData(OptionType.STRING, "duration", "moderation_duration", false),
            )

        val unlockTrigger = Command.getCommandProperties(UnlockCommand::class.java).trigger
        val unlockSubcommandData = generateSubcommandData(unlockTrigger, "${unlockTrigger}_description")
            .addOptions(generateOptionData(OptionType.CHANNEL, "channel", "lock_slash_channel", false))

        commandData.addSubcommands(lockSubcommandData, unlockSubcommandData)
        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}