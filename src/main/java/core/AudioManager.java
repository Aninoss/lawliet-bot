package core;

import java.util.concurrent.CompletableFuture;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.io.jda.JdaLavalink;

public class AudioManager {

    private static JdaLavalink jdaLavalink;
    private static boolean started = false;

    public static synchronized void init(int totalShards) {
        jdaLavalink = new JdaLavalink(totalShards);
    }

    public static synchronized void start(long userId) {
        if (!started) {
            started = true;
            jdaLavalink.setUserId(String.valueOf(userId));
            jdaLavalink.setJdaProvider(shardId -> ShardManager.getJDA(shardId).orElse(null));
        }
    }

    public static JdaLavalink getJdaLavalink() {
        return jdaLavalink;
    }

    public static CompletableFuture<AudioTrack> fetchAudioTrack(String search, AudioPlayerManager playerManager) {
        CompletableFuture<AudioTrack> future = new CompletableFuture<>();
        playerManager.loadItem(search, generateAudioLoadResultHandler(future));
        return future;
    }

    private static AudioLoadResultHandler generateAudioLoadResultHandler(CompletableFuture<AudioTrack> future) {
        return new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                future.complete(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack track = playlist.getSelectedTrack();
                if (track == null && playlist.getTracks().size() > 0) {
                    track = playlist.getTracks().get(0);
                }
                future.complete(track);
            }

            @Override
            public void noMatches() {
                future.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                future.completeExceptionally(exception);
            }
        };
    }

}
