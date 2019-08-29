package General.AutoChannel;

import org.javacord.api.entity.channel.ServerVoiceChannel;

/**
 * Jeweils ein Objekt pro erstellten temporären VC. Hier wird der erstellte Channel sowie das Original gespeichert.
 */
public class TempAutoChannel {
    private ServerVoiceChannel tempChannel;
    private ServerVoiceChannel originalChannel;

    /**
     * Jeweils ein Objekt pro erstellten temporären VC. Hier wird der erstellte Channel sowie das Original gespeichert.
     * @param originalChannel Der originale Auto Channel
     * @param tempChannel Der temporäre Auto Channel
     */
    public TempAutoChannel(ServerVoiceChannel originalChannel, ServerVoiceChannel tempChannel) {
        this.originalChannel = originalChannel;
        this.tempChannel = tempChannel;
    }

    /**
     * Gibt den entsprechenden temporären Channel zurück.
     * @return ServerVoiceChannel
     */
    public ServerVoiceChannel getTempChannel() {
        return tempChannel;
    }

    /**
     * Gibt den entsprechenden original Channel zurück.
     * @return ServerVoiceChannel
     */
    public ServerVoiceChannel getOriginalChannel() {
        return originalChannel;
    }
}