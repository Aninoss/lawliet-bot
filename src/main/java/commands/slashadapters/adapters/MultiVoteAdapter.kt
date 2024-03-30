package commands.slashadapters.adapters

import commands.runnables.utilitycategory.MultiVoteCommand
import commands.runnables.utilitycategory.VoteCommand
import commands.slashadapters.Slash

@Slash(command = MultiVoteCommand::class)
class MultiVoteAdapter : VoteAdapter() {

    override fun getVoteCommandClass(): Class<out VoteCommand> {
        return MultiVoteCommand::class.java
    }

}