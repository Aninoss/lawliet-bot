package commands.stateprocessor;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class StringStateProcessor extends AbstractStateProcessor<String, StringStateProcessor> {

    private int max = MessageEmbed.VALUE_MAX_LENGTH;

    public StringStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_text_desc"));
    }

    public StringStateProcessor setMax(int max) {
        this.max = max;
        return this;
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
