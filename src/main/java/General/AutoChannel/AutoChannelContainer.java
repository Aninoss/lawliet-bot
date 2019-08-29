package General.AutoChannel;

import org.javacord.api.entity.channel.ServerVoiceChannel;

import java.util.ArrayList;

/**
 * Speichert alle aktiven Auto Channels
 */
public class AutoChannelContainer {
    private static AutoChannelContainer ourInstance = new AutoChannelContainer();
    private ArrayList<TempAutoChannel> channelList = new ArrayList<>();

    public static AutoChannelContainer getInstance() {
        return ourInstance;
    }

    /**
     * Speichert alle aktiven Auto Channels
     */
    private AutoChannelContainer() {}

    /**
     * Fügt einen Voice Channel in die Liste hinzu
     *
     * @param tempAutoChannel Ein TempAutoChannel-Object
     * @return Das hinzugefügte TempAutoChannel-Object
     */
    public TempAutoChannel addVoiceChannel(TempAutoChannel tempAutoChannel) {
        channelList.add(tempAutoChannel);
        return tempAutoChannel;
    }

    /**
     * Entfernt einen Voice Channel aus der Liste
     *
     * @param tempChannel Der temporäre Channel als ServerVoiceChannel
     */
    public void removeVoiceChannel(ServerVoiceChannel tempChannel) {
        TempAutoChannel deleteObject = null;
        for(TempAutoChannel tempAutoChannel: channelList) {
            if (tempAutoChannel.getTempChannel() == tempChannel) {
                deleteObject = tempAutoChannel;
                break;
            }
        }
        if (deleteObject != null) channelList.remove(deleteObject);
    }

    /**
     * Gibt die Größe der Voice Channel Liste zurück
     *
     * @param originalChannel Der originale ServerVoiceChannel des Temporären
     * @return int
     */
    public int getSize(ServerVoiceChannel originalChannel) {
        int counter = 0;
        for(TempAutoChannel tempAutoChannel: channelList) {
            if (tempAutoChannel.getOriginalChannel() == originalChannel)
                counter++;
        }
        return counter;
    }

    /**
     * Gibt die Größe der Voice Channel Liste zurück
     * @return int
     */
    public int getSize() {
        return channelList.size();
    }

    /**
     * Gibt die aktuelle Voice Channel Liste zurück
     */
    public ArrayList<TempAutoChannel> getChannelList() {
        return channelList;
    }

    /**
     * Gibt den TempAutoChannel entsprechend des VoiceServerChannels zurück
     *
     * @param tempChannel Der originale ServerVoiceChannel des Temporären
     * @return boolean
     */
    public TempAutoChannel getTempAutoChannel(ServerVoiceChannel tempChannel) {
        for(TempAutoChannel tempAutoChannel: channelList) {
            if (tempAutoChannel.getTempChannel().getId() == tempChannel.getId())
                return tempAutoChannel;
        }
        return null;
    }
}