package commands.stateprocessor;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class FileStateProcessor extends AbstractStateProcessor<Message.Attachment, Message.Attachment, FileStateProcessor> {

    public FileStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_file_desc"));
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
