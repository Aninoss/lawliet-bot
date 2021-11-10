package commands.slashadapters.adapters

import commands.runnables.nsfwcategory.E621Command
import commands.slashadapters.Slash

@Slash(command = E621Command::class)
class E621Adapter : BooruSearchAdapterAbstract()