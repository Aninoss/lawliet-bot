package mysql.modules.tracker;

public class TrackerKey {

    private final long channelId;
    private final String commandTrigger;
    private final String commandKey;

    public TrackerKey(long channelId, String commandTrigger, String commandKey) {
        this.channelId = channelId;
        this.commandTrigger = commandTrigger;
        this.commandKey = commandKey;
    }

    public long getChannelId() {
        return channelId;
    }

    public String getCommandTrigger() {
        return commandTrigger;
    }

    public String getCommandKey() {
        return commandKey;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TrackerKey) {
            TrackerKey other = (TrackerKey)obj;
            return channelId == other.channelId &&
                    commandTrigger.equals(other.commandTrigger) &&
                    commandKey.equals(other.commandKey);
        }
        return false;
    }

}
