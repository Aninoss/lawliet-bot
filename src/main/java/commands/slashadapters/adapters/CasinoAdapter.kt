package commands.slashadapters.adapters;

import commands.Category;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.runnables.CasinoAbstract;
import commands.runnables.CasinoMultiplayerAbstract;
import commands.runnables.casinocategory.CoinFlipCommand;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import constants.Language;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

@Slash(name = "casino", description = "Bet your coins in virtual gambling games")
public class CasinoAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        for (Class<? extends Command> clazz : CommandContainer.getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, Language.EN.getLocale(), "/");
            if (command.getCategory() == Category.CASINO) {
                SubcommandData subcommandData = new SubcommandData(command.getCommandProperties().trigger(), command.getCommandLanguage().getDescShort());
                if (command instanceof CasinoMultiplayerAbstract ||
                        (command instanceof CasinoAbstract && ((CasinoAbstract) command).allowBet())
                ) {
                    subcommandData.addOption(OptionType.STRING, "bet", "The number of coins you want to bet on", false);
                }
                if (command instanceof CoinFlipCommand) {
                    OptionData optionData = new OptionData(OptionType.STRING, "selection", "Select head or tails for your coin toss", false)
                            .addChoice("head", "head")
                            .addChoice("tails", "tails");
                    subcommandData.addOptions(optionData);
                }
                commandData.addSubcommands(subcommandData);
            }
        }

        return commandData;
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        String trigger = event.getSubcommandName();
        Class<? extends Command> clazz = CommandContainer.getCommandMap().get(trigger);
        return new SlashMeta(clazz, collectArgs(event));
    }

}
