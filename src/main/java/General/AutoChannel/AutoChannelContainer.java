package General.AutoChannel;

import org.javacord.api.entity.channel.ServerVoiceChannel;

import java.util.ArrayList;

public class AutoChannelContainer {

    private static AutoChannelContainer ourInstance = new AutoChannelContainer();
    private ArrayList<TempAutoChannel> channelList = new ArrayList<>();

    public static AutoChannelContainer getInstance() {
        return ourInstance;
    }

    public TempAutoChannel addVoiceChannel(TempAutoChannel tempAutoChannel) {
        channelList.add(tempAutoChannel);
        return tempAutoChannel;
    }

    public void removeVoiceChannel(ServerVoiceChannel tempChannel) {
        TempAutoChannel deleteObject = null;
        synchronized (this) {
            for (TempAutoChannel tempAutoChannel : channelList) {
                if (tempAutoChannel.getTempChannel() == tempChannel) {
                    deleteObject = tempAutoChannel;
                    break;
                }
            }
            if (deleteObject != null) channelList.remove(deleteObject);
        }
    }

    public int getSize(ServerVoiceChannel originalChannel) {
        int counter = 0;
        for(TempAutoChannel tempAutoChannel: channelList) {
            if (tempAutoChannel.getOriginalChannel() == originalChannel)
                counter++;
        }
        return counter;
    }

    public int getSize() {
        return channelList.size();
    }

    public ArrayList<TempAutoChannel> getChannelList() {
        return channelList;
    }

    public TempAutoChannel getTempAutoChannel(ServerVoiceChannel tempChannel) {
        for(TempAutoChannel tempAutoChannel: channelList) {
            if (tempAutoChannel.getTempChannel().getId() == tempChannel.getId())
                return tempAutoChannel;
        }
        return null;
    }

    public void removeShard(int shardId) {
        channelList.removeIf(tempAutoChannel -> tempAutoChannel.getOriginalChannel().getApi().getCurrentShard() == shardId);
    }

}