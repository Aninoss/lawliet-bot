package mysql.modules.staticreactionmessages;

import core.assets.MessageAsset;
import net.dv8tion.jda.api.entities.Message;

public class StaticReactionMessageData implements MessageAsset {

    private final long guildId;
    private final long channelId;
    private final long messageId;
    private final String command;
    private final String secondaryId;

    public StaticReactionMessageData(Message message, String command) {
        this.guildId = message.getGuild().getIdLong();
        this.channelId = message.getChannel().getIdLong();
        this.messageId = message.getIdLong();
        this.command = command;
        this.secondaryId = null;
    }

    public StaticReactionMessageData(Message message, String command, String secondaryId) {
        this.guildId = message.getGuild().getIdLong();
        this.channelId = message.getChannel().getIdLong();
        this.messageId = message.getIdLong();
        this.command = command;
        this.secondaryId = secondaryId;
    }

    public StaticReactionMessageData(long guildId, long channelId, long messageId, String command) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.command = command;
        this.secondaryId = null;
    }

    public StaticReactionMessageData(long guildId, long channelId, long messageId, String command, String secondaryId) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.command = command;
        this.secondaryId = secondaryId;
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

}
