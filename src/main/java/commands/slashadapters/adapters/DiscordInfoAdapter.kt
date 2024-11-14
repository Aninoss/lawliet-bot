package commands.slashadapters.adapters

import commands.Category
import commands.Command
import commands.CommandContainer
import commands.runnables.informationcategory.ChannelInfoCommand
import commands.runnables.informationcategory.ServerInfoCommand
import commands.runnables.informationcategory.UserInfoCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(
    name = "discordinfo",
    descriptionCategory = [Category.INFORMATION],
    descriptionKey = "info_discordinfo_desc",
    commandAssociations = [ ServerInfoCommand::class, ChannelInfoCommand::class, UserInfoCommand::class ]
)
class DiscordInfoAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val serverInfoTrigger = Command.getCommandProperties(ServerInfoCommand::class.java).trigger
        commandData.addSubcommands(
            generateSubcommandData(serverInfoTrigger, "${serverInfoTrigger}_description")
        )

        val channelInfoTrigger = Command.getCommandProperties(ChannelInfoCommand::class.java).trigger
        commandData.addSubcommands(
            generateSubcommandData(channelInfoTrigger, "${channelInfoTrigger}_description")
                .addOptions(generateOptionData(OptionType.CHANNEL, "channel", "info_channel", false))
        )

        val userInfoTrigger = Command.getCommandProperties(UserInfoCommand::class.java).trigger
        commandData.addSubcommands(
            generateSubcommandData(userInfoTrigger, "${userInfoTrigger}_description")
                .addOptions(generateOptionData(OptionType.USER, "member", "info_member", false))
        )

        return commandData
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val trigger = event.subcommandName
        val clazz = CommandContainer.getCommandMap()[trigger]!!
        return SlashMeta(clazz, collectArgs(event))
    }

}