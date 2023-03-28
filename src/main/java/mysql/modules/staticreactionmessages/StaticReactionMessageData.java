package mysql.modules.staticreactionmessages;

import core.assets.MessageAsset;
import net.dv8tion.jda.api.entities.Message;

public class StaticReactionMessageData implements MessageAsset {

    private final long guildId;
    private final long channelId;
    private final long messageId;
    private final String command;
    private final String secondaryId;
    private final Runnable runAfterSave;

    public StaticReactionMessageData(Message message, String command) {
        this(message, command, null, null);
    }

    public StaticReactionMessageData(Message message, String command, String secondaryId) {
        this(message, command, secondaryId, null);
    }

    public StaticReactionMessageData(Message message, String command, String secondaryId, Runnable runAfterSave) {
        this.guildId = message.getGuild().getIdLong();
        this.channelId = message.getChannel().getIdLong();
        this.messageId = message.getIdLong();
        this.command = command;
        this.secondaryId = secondaryId;
        this.runAfterSave = runAfterSave;
    }

    public StaticReactionMessageData(long guildId, long channelId, long messageId, String command) {
        this(guildId, channelId, messageId, command, null, null);
    }

    public StaticReactionMessageData(long guildId, long channelId, long messageId, String command, String secondaryId) {
        this(guildId, channelId, messageId, command, secondaryId, null);
    }

    public StaticReactionMessageData(long guildId, long channelId, long messageId, String command, String secondaryId, Runnable runAfterSave) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.command = command;
        this.secondaryId = secondaryId;
        this.runAfterSave = runAfterSave;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public long getGuildId() {
        return guildId;
    }

    @Override
    public long getMessageId() {
        return messageId;
    }

    @Override
    public long getStandardGuildMessageChannelId() {
        return channelId;
    }

    public String getSecondaryId() {
        return secondaryId;
    }

    public Runnable getRunAfterSave() {
        return runAfterSave;
    }

}
