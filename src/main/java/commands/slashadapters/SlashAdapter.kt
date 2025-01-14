package commands.slashadapters

import commands.Category
import commands.Command
import commands.CommandContainer
import constants.Language
import core.TextManager
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import okhttp3.internal.toImmutableList
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

abstract class SlashAdapter {

    protected abstract fun addOptions(commandData: SlashCommandData): SlashCommandData

    abstract fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta

    fun name(): String {
        val slash = javaClass.getAnnotation(Slash::class.java)
        var name = slash.name
        if (name.isEmpty()) {
            name = Command.getCommandProperties(slash.command.java).trigger
        }
        return name
    }

    fun description(): String {
        val slash = javaClass.getAnnotation(Slash::class.java)
        if (slash.descriptionKey.isEmpty()) {
            val trigger = name()
            val clazz = CommandContainer.getCommandMap()[trigger]!!
            val category = Command.getCategory(clazz)
            return TextManager.getString(Language.EN.locale, category, trigger + "_description")
        } else {
            return TextManager.getString(Language.EN.locale, slash.descriptionCategory[0], slash.descriptionKey)
        }
    }

    fun descriptionCategory(): Category {
        val slash = javaClass.getAnnotation(Slash::class.java)
        if (slash.descriptionCategory.isEmpty()) {
            val clazz = CommandContainer.getCommandMap()[name()]!!
            return Command.getCategory(clazz)
        } else {
            return slash.descriptionCategory[0]
        }
    }

    fun descriptionLocalizations(): Map<DiscordLocale, String> {
        val localizationMap = HashMap<DiscordLocale, String>()
        val slash = javaClass.getAnnotation(Slash::class.java)
        if (slash.descriptionKey.isEmpty()) {
            val trigger = name()
            val clazz = CommandContainer.getCommandMap()[trigger]!!
            val category = Command.getCategory(clazz)
            Language.values().filter { it != Language.EN }
                .forEach {
                    val description = TextManager.getString(it.locale, category, trigger + "_description")
                    localizationMap.put(it.discordLocales[0], description)
                }
        } else {
            Language.values().filter { it != Language.EN }
                .forEach {
                    val description = TextManager.getString(it.locale, slash.descriptionCategory[0], slash.descriptionKey)
                    localizationMap.put(it.discordLocales[0], description)
                }
        }
        return localizationMap
    }

    fun requiredPermissions(): Collection<Permission> {
        val permissions = HashSet<Permission>()
        val slash = javaClass.getAnnotation(Slash::class.java)
        permissions += slash.permissions
        if (slash.command != Command::class) {
            val commandProperties = Command.getCommandProperties(slash.command.java)
            permissions += commandProperties.userGuildPermissions
            permissions += commandProperties.userChannelPermissions
        }
        return permissions
    }

    fun commandClass(): KClass<out Command> {
        val slash = javaClass.getAnnotation(Slash::class.java)
        return slash.command
    }

    fun commandAssociations(): Array<KClass<out Command>> {
        val slash = javaClass.getAnnotation(Slash::class.java)
        return slash.commandAssociations
    }

    fun commandAssociationCategories(): Array<Category> {
        val slash = javaClass.getAnnotation(Slash::class.java)
        return slash.commandAssociationCategories
    }

    fun messageCommandAssociations(): List<String> {
        val list = ArrayList<String>()

        val commandClass = commandClass()
        if (!commandClass.jvmName.equals(Command::class.jvmName)) {
            val trigger = Command.getCommandProperties(commandClass.java).trigger
            list += trigger
        }
        list += commandAssociations()
            .map { Command.getCommandProperties(it.java).trigger }
        list += commandAssociationCategories()
            .map { it.id }

        return list.toImmutableList()
    }

    fun nsfw(): Boolean {
        val slash = javaClass.getAnnotation(Slash::class.java)
        if (slash.command != Command::class) {
            return Command.getCommandProperties(slash.command.java).nsfw
        }
        return slash.nsfw
    }

    fun onlyPublicVersion(): Boolean {
        val slash = javaClass.getAnnotation(Slash::class.java)
        if (slash.command != Command::class) {
            return Command.getCommandProperties(slash.command.java).onlyPublicVersion
        }
        return slash.onlyPublicVersion
    }

    fun generateCommandData(): SlashCommandData {
        val commandData = Commands.slash(name(), description())
        commandData.setDescriptionLocalizations(descriptionLocalizations())
        commandData.isGuildOnly = true
        commandData.defaultPermissions = DefaultMemberPermissions.enabledFor(requiredPermissions())
        commandData.isNSFW = nsfw()
        return addOptions(commandData)
    }

    fun generateOptionData(type: OptionType, name: String, descriptionKey: String, required: Boolean = false, autoComplete: Boolean = false): OptionData {
        val descriptionCategory = descriptionCategory()
        return generateOptionData(type, name, descriptionCategory.id, descriptionKey, required, autoComplete)
    }

    fun generateOptionData(type: OptionType, name: String, descriptionCategory: String, descriptionKey: String, required: Boolean = false, autoComplete: Boolean = false): OptionData {
        val descriptionEnglish = TextManager.getString(Language.EN.locale, descriptionCategory, descriptionKey)
        val optionData = OptionData(type, name, descriptionEnglish, required, autoComplete)
        Language.values().filter { it != Language.EN }
            .forEach {
                val description = TextManager.getString(it.locale, descriptionCategory, descriptionKey)
                optionData.setDescriptionLocalization(it.discordLocales[0], description)
            }

        return optionData
    }

    fun generateChoice(nameKey: String, value: String): Choice {
        val nameCategory = descriptionCategory()
        return generateChoice(nameCategory.id, nameKey, value)
    }

    fun generateChoice(nameCategory: String, nameKey: String, value: String): Choice {
        val nameEnglish = TextManager.getString(Language.EN.locale, nameCategory, nameKey)
        val choice = Choice(nameEnglish, value)
        Language.values().filter { it != Language.EN }
            .forEach {
                val name = TextManager.getString(it.locale, nameCategory, nameKey)
                choice.setNameLocalization(it.discordLocales[0], name)
            }

        return choice
    }

    fun generateSubcommandData(name: String, descriptionKey: String): SubcommandData {
        val descriptionCategory = descriptionCategory()
        return generateSubcommandData(name, descriptionCategory.id, descriptionKey)
    }

    fun generateSubcommandData(name: String, descriptionCategory: String, descriptionKey: String): SubcommandData {
        val descriptionEnglish = TextManager.getString(Language.EN.locale, descriptionCategory, descriptionKey)
        val subcommandData = SubcommandData(name, descriptionEnglish)
        Language.values().filter { it != Language.EN }
            .forEach {
                val description = TextManager.getString(it.locale, descriptionCategory, descriptionKey)
                subcommandData.setDescriptionLocalization(it.discordLocales[0], description)
            }

        return subcommandData
    }

    open fun retrieveChoices(event: CommandAutoCompleteInteractionEvent, guildEntity: GuildEntity): List<Choice> {
        return emptyList()
    }

    companion object {

        @JvmStatic
        protected fun collectArgs(event: SlashCommandInteractionEvent, vararg exceptions: String): String {
            val argsBuilder = StringBuilder()
            for (option in event.options) {
                if (Arrays.stream(exceptions).noneMatch { exception: String -> option.name == exception }) {
                    when (option.type) {
                        OptionType.BOOLEAN -> {
                            if (option.asBoolean) {
                                argsBuilder.append(option.name).append(" ")
                            }
                        }
                        OptionType.ATTACHMENT -> argsBuilder.append(option.asAttachment.url).append(" ")
                        else -> argsBuilder.append(option.asString).append(" ")
                    }
                }
            }
            return argsBuilder.toString()
        }

    }
}