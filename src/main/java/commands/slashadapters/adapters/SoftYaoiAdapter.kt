package commands.slashadapters.adapters

import commands.runnables.externalcategory.SoftYaoiCommand
import commands.slashadapters.Slash

@Slash(command = SoftYaoiCommand::class)
class SoftYaoiAdapter : BooruPredeterminedAdapterAbstract()