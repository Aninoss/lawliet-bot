package mysql.modules.staticreactionmessages;

import core.assets.MessageAsset;
import net.dv8tion.jda.api.entities.Message;

public class StaticReactionMessageData implements MessageAsset {

    private final long guildId;
    private final long channelId;
    private final long messageId;
    private final String command;

    public StaticReactionMessageData(Message message, String command) {
        this.guildId = message.getGuild().getIdLong();
        this.channelId = message.getChannel().getIdLong();
        this.messageId = message.getIdLong();
        this.command = command;
    }

    public StaticReactionMessageData(long guildId, long channelId, long messageId, String command) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.command = command;
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
    public long getBaseGuildMessageChannelId() {
        return channelId;
    }

}
