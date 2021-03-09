package modules;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.extern.log4j.Log4j2;

public class YouTubeMeta {

    private static final YouTubeMeta ourInstance = new YouTubeMeta();

    public static YouTubeMeta getInstance() {
        return ourInstance;
    }

    private final AudioPlayerManager playerManager = new DefaultAudioPlayerManager();

    private YouTubeMeta() {
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
    }

    public Optional<AudioTrackInfo> getFromVideoURL(String url) throws ExecutionException, InterruptedException {
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


}
