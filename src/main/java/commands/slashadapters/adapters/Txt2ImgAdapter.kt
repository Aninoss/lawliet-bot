package commands.slashadapters.adapters

import commands.runnables.aitoyscategory.Txt2ImgCommand
import commands.slashadapters.Slash

@Slash(command = Txt2ImgCommand::class)
class Txt2ImgAdapter : RunPodAdapterAbstract()