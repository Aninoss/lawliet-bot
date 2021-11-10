package commands.slashadapters.adapters

import commands.runnables.nsfwcategory.Rule34Command
import commands.slashadapters.Slash

@Slash(command = Rule34Command::class)
class Rule34Adapter : BooruSearchAdapterAbstract()