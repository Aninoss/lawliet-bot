package commands.slashadapters.adapters;

import java.util.ArrayList;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.runnables.PornPredefinedAbstract;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import constants.Language;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

@Slash(name = "nsfw", description = "Find nsfw content for predefined tags")
public class NSFWAdapter extends SlashAdapter {

    public CommandData addOptions(CommandData commandData) {
        OptionData triggerOptionData = new OptionData(OptionType.STRING, "command", "Which nsfw command do you want to run?", true);
        ArrayList<String> triggerList = new ArrayList<>();
        for (Class<? extends Command> clazz : CommandContainer.getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, Language.EN.getLocale(), "/");
            if (command instanceof PornPredefinedAbstract && command.getCommandProperties().nsfw()) {
                triggerList.add(command.getTrigger());
            }
        }
        triggerList.sort(String::compareTo);
        for (String trigger : triggerList) {
            triggerOptionData.addChoice(trigger, trigger);
        }

        return commandData
                .addOptions(triggerOptionData)
                .addOption(OptionType.INTEGER, "amount", "Amount of posts from 1 to 20 / 30", false);
    }

    @Override
    public SlashMeta process(SlashCommandEvent event) {
        Class<? extends Command> clazz = CommandContainer.getCommandMap().get(event.getOption("command").getAsString());
        return new SlashMeta(clazz, collectArgs(event, "command"));
    }

}
