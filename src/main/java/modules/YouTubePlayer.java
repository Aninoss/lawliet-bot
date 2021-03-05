package modules;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import core.LavaplayerAudioSource;
import core.MainLogger;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.audio.AudioSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class YouTubePlayer {

    private static final YouTubePlayer ourInstance = new YouTubePlayer();

    public static YouTubePlayer getInstance() {
        return ourInstance;
    }

    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    private YouTubePlayer() {
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
    }

    public Optional<AudioTrackInfo> meta(String url) throws ExecutionException, InterruptedException {
        AtomicReference<Optional<AudioTrackInfo>> trackInfoAtomic = new AtomicReference<>(Optional.empty());

        playerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                trackInfoAtomic.set(Optional.of(track.getInfo()));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
            }

            @Override
            public void noMatches() {
            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                MainLogger.get().error("Could not load music track", throwable);
            }
        }).get();

        return trackInfoAtomic.get();
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
                MainLogger.get().error("Could not load music track", throwable);
            }
        });
    }


}
