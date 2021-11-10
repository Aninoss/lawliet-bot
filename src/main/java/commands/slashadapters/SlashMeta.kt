package commands.slashadapters

import commands.Command
import java.util.*
import java.util.function.Function

class SlashMeta @JvmOverloads constructor(
    val commandClass: Class<out Command>,
    val args: String,
    val errorFunction: Function<Locale, String>? = null
)