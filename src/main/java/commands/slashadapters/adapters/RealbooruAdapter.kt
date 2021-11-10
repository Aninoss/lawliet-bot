package commands.slashadapters.adapters

import commands.runnables.nsfwcategory.RealbooruCommand
import commands.slashadapters.Slash

@Slash(command = RealbooruCommand::class)
class RealbooruAdapter : BooruSearchAdapterAbstract()