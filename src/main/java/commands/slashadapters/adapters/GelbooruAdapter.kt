package commands.slashadapters.adapters

import commands.runnables.nsfwcategory.GelbooruCommand
import commands.slashadapters.Slash

@Slash(command = GelbooruCommand::class)
class GelbooruAdapter : BooruSearchAdapterAbstract()