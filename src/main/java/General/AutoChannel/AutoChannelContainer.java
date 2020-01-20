package General.AutoChannel;

import General.DiscordApiCollection;
import org.javacord.api.entity.channel.ServerVoiceChannel;

import java.util.ArrayList;

public class AutoChannelContainer {

    private static AutoChannelContainer ourInstance = new AutoChannelContainer();
    private ArrayList<TempAutoChannel> channelList = new ArrayList<>();

    public static AutoChannelContainer getInstance() {
        return ourInstance;
    }

    public synchronized void addVoiceChannel(TempAutoChannel tempAutoChannel) {
        channelList.add(tempAutoChannel);
    }

    public void removeVoiceChannel(long tempChannelId) {
        synchronized (this) {
            for (TempAutoChannel tempAutoChannel : new ArrayList<>(channelList)) {
                if (tempAutoChannel.getTempChannelId() == tempChannelId) {
                    channelList.remove(tempAutoChannel);
                    break;
                }
            }
        }
    }

    public int getSize(ServerVoiceChannel originalChannel) {
        int counter = 0;
        for(TempAutoChannel tempAutoChannel: channelList) {
            if (tempAutoChannel.getOriginalChannelId() == originalChannel.getId())
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
            if (tempAutoChannel.getTempChannelId() == tempChannel.getId())
                return tempAutoChannel;
        }
        return null;
    }

    public void removeShard(int shardId) {
        channelList.removeIf(tempAutoChannel -> DiscordApiCollection.getInstance().getResponsibleShard(tempAutoChannel.getServerId()) == shardId);
    }

}