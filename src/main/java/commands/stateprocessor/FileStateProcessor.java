package commands.stateprocessor;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.function.Consumer;

public class FileStateProcessor extends AbstractStateProcessor<Message.Attachment, Message.Attachment> {

    public FileStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, boolean canClear, Consumer<Message.Attachment> setter) {
        this(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_file_desc"), canClear, setter);
    }

    public FileStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, String description, boolean canClear, Consumer<Message.Attachment> setter) {
        super(command, state, stateBack, propertyName, description, canClear, setter);
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input) {
        if (event.getMessage().getAttachments().isEmpty()) {
            NavigationAbstract command = getCommand();
            command.setLog(LogStatus.FAILURE, TextManager.getNoResultsString(command.getLocale(), input));
            return MessageInputResponse.FAILED;
        }

        set(event.getMessage().getAttachments().get(0));
        return MessageInputResponse.SUCCESS;
    }

}
