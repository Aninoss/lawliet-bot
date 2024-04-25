package commands.stateprocessor;

import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.utils.InternetUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class FileStateProcessor extends AbstractStateProcessor<String, Message.Attachment, FileStateProcessor> {

    private boolean allowGifs = true;

    public FileStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_file_desc"));
    }

    public FileStateProcessor setAllowGifs(boolean supportGifs) {
        this.allowGifs = supportGifs;
        return this;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input) {
        if (event.getMessage().getAttachments().isEmpty() || !InternetUtil.uriIsImage(event.getMessage().getAttachments().get(0).getUrl(), allowGifs)) {
            NavigationAbstract command = getCommand();
            command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, allowGifs ? "imagenotfound" : "imagenotfound_nogifs"));
            return MessageInputResponse.FAILED;
        }

        set(event.getMessage().getAttachments().get(0));
        return MessageInputResponse.SUCCESS;
    }

}
