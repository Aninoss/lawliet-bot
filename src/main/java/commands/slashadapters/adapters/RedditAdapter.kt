package commands.slashadapters.adapters

import commands.runnables.externalcategory.RedditCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import core.utils.JDAUtil
import modules.reddit.RedditAutoComplete
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = RedditCommand::class)
class RedditAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val orderBy = generateOptionData(OptionType.STRING, "sort_by", "reddit_sortby", false).addChoices(
            generateChoice("reddit_hot", "hot"),
            generateChoice("reddit_new", "new"),
            generateChoice("reddit_top", "top"),
            generateChoice("reddit_rising", "rising")
        )
        return commandData
            .addOptions(
                generateOptionData(OptionType.STRING, "subreddit", "reddit_subreddit", true, true),
                orderBy
            )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        var args = event.getOption("subreddit")!!.asString
        if (event.getOption("sort_by") != null) {
            args += "/" + event.getOption("sort_by")!!.asString
        }
        return SlashMeta(RedditCommand::class.java, args)
    }

    override fun retrieveChoices(event: CommandAutoCompleteInteractionEvent, guildEntity: GuildEntity): List<Command.Choice> {
        val query = event.focusedOption.value
        if (query.isBlank()) {
            return emptyList()
        }

        val allowNsfw = JDAUtil.channelIsNsfw(event.channel)
        return redditAutoComplete.getAutoComplete(query).get()
            .filter { !it.isNsfw || allowNsfw }
            .map { Command.Choice("${it.name} (${it.subscribers})", it.name) }
    }

    companion object {

        val redditAutoComplete = RedditAutoComplete()

    }
}