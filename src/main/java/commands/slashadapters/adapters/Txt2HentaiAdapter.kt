package commands.slashadapters.adapters

import commands.runnables.nsfwcategory.Txt2HentaiCommand
import commands.slashadapters.Slash

@Slash(command = Txt2HentaiCommand::class, nsfw = true)
class Txt2HentaiAdapter : RunPodAdapterAbstract()