package commands.stateprocessor;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.function.Consumer;

public class StringStateProcessor extends AbstractStateProcessor<String, String> {

    private final int max;

    public StringStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, int max, boolean canClear, Consumer<String> setter) {
        this(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_text_desc"), max, canClear, setter);
    }

    public StringStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, String description, int max, boolean canClear, Consumer<String> setter) {
        super(command, state, stateBack, propertyName, description, canClear, setter);
        this.max = max;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input) {
        if (input.length() > max) {
            NavigationAbstract command = getCommand();
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "too_many_characters", StringUtil.numToString(max)));
            return MessageInputResponse.FAILED;
        }

        set(input);
        return MessageInputResponse.SUCCESS;
    }

}
