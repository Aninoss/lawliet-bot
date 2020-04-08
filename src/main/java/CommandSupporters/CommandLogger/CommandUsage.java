package CommandSupporters.CommandLogger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommandUsage {

    private String messageContent;
    public enum Result { SUCCESS, FALSE, EXCEPTION }
    private Result result;
    private LocalDateTime localDateTime = LocalDateTime.now();

    public CommandUsage(String messageContent, Result result) {
        this.messageContent = messageContent;
        this.result = result;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public Result getResult() {
        return result;
    }

    public String getInstantString() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return dtf.format(localDateTime);
    }
}