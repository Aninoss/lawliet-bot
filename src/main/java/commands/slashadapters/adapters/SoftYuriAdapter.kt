package commands.slashadapters.adapters

import commands.runnables.externalcategory.SoftYuriCommand
import commands.slashadapters.Slash

@Slash(command = SoftYuriCommand::class)
class SoftYuriAdapter : BooruPredeterminedAdapterAbstract()