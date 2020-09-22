package modules;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import core.LavaplayerAudioSource;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MusicPlayer {

    private final static Logger LOGGER = LoggerFactory.getLogger(MusicPlayer.class);

    private static final MusicPlayer ourInstance = new MusicPlayer();
    public static MusicPlayer getInstance() { return ourInstance; }

    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    private MusicPlayer() {
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
    }

    public void play(AudioConnection audioConnection, String url) {
        DiscordApi api = audioConnection.getChannel().getApi();
        AudioPlayer player = playerManager.createPlayer();

        AudioSource source = new LavaplayerAudioSource(api, player);
        audioConnection.setAudioSource(source);

        playerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                player.playTrack(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    player.playTrack(track);
                }
            }

            @Override
            public void noMatches() {
                // Notify the user that we've got nothing
            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                LOGGER.error("Could not load music track", throwable);
            }
        });
    }


}
