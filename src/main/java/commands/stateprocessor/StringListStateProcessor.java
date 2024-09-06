package commands.stateprocessor;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StringListStateProcessor extends AbstractStateProcessor<List<String>, List<String>, StringListStateProcessor> {

    private int maxEntries = Integer.MAX_VALUE;
    private int maxLength = Button.LABEL_MAX_LENGTH;
    private Function<String, List<String>> stringSplitterFunction;

    public StringListStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_stringlist_desc"));
        setClearButton(false);
    }

    public StringListStateProcessor setMax(int maxEntries, int maxLength) {
        this.maxEntries = maxEntries;
        this.maxLength = maxLength;
        return this;
    }

    public StringListStateProcessor setStringSplitterFunction(Function<String, List<String>> stringSplitterFunction) {
        this.stringSplitterFunction = stringSplitterFunction;
        return this;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input) {
        List<String> newEntries = stringSplitterFunction.apply(input).stream()
                .map(str -> str.substring(0, Math.min(str.length(), maxLength)))
                .filter(str -> !str.isBlank())
                .collect(Collectors.toList());

        List<String> previousEntries = get();
        if (previousEntries.size() >= maxEntries) {
            NavigationAbstract command = getCommand();
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_stringlist_toomanyentries", StringUtil.numToString(maxEntries)));
            return MessageInputResponse.FAILED;
        }

        ArrayList<String> entries = new ArrayList<>(previousEntries);
        newEntries.stream()
                .limit(maxEntries - previousEntries.size())
                .forEach(entry -> {
                    if (!entries.contains(entry)) {
                        entries.add(entry);
                    }
                });
        set(entries, false);
        return MessageInputResponse.SUCCESS;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i) throws Throwable {
        if (i == -1) {
            return super.controllerButton(event, i);
        }

        ArrayList<String> entries = new ArrayList<>(get());
        entries.remove(i);
        set(entries, false);
        return true;
    }

    @Override
    protected void addComponents(NavigationAbstract command) {
        command.setComponents(get().stream().map(str -> str + " ✕").toArray(String[]::new));
    }

}
