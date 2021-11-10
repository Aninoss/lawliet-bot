package commands.slashadapters.adapters

import commands.runnables.externalcategory.SafebooruCommand
import commands.slashadapters.Slash

@Slash(command = SafebooruCommand::class)
class SafeBooruAdapter : BooruSearchAdapterAbstract()