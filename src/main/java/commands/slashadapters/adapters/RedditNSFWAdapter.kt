package commands.slashadapters.adapters

import commands.Category
import commands.runnables.nsfwcategory.RedditNSFWCommand
import commands.slashadapters.Slash
import core.utils.JDAUtil
import modules.reddit.RedditAutoComplete
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = RedditNSFWCommand::class)
class RedditNSFWAdapter : BooruSearchAdapterAbstract() {

    override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(
                generateOptionData(OptionType.STRING, "subreddit", Category.EXTERNAL.id, "reddit_subreddit", true, true),
                generateOptionData(OptionType.INTEGER, "amount", Category.NSFW.id, "porn_amount", false)
            )
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent, guildEntity: GuildEntity): List<Command.Choice> {
        val query = event.focusedOption.value
        if (query.isBlank()) {
            return emptyList()
        }

        val allowNsfw = JDAUtil.channelIsNsfw(event.channel)
        return RedditAutoComplete.getAutoComplete(query).get()
            .filter { !it.isNsfw || allowNsfw }
            .map { Command.Choice("${it.name} (${it.subscribers})", it.name) }
    }

}