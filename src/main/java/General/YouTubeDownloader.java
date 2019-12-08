package General;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YouTubeDownloader {

    private static ArrayList<String> videoRequests = new ArrayList<>();

    public static Optional<String> getVideoID(String str) {
        String[] starters = {"v=", "youtu.be/"};
        for(String starter: starters) {
            if (str.contains(starter)) {
                str += "&";
                str = Tools.cutString(str, starter, "&");
                try {
                    YoutubeDownloader.getVideo(str);
                } catch (IOException | YoutubeException e) {
                    //Ignore
                    return Optional.empty();
                }

                return Optional.of(str);
            }
        }

        return Optional.empty();
    }

    public static File downloadAudio(String videoId) throws IOException, YoutubeException {
        if (videoRequests.contains(videoId)) {
            File file;
            while(!(file = new File("temp/" + videoId + ".mp3")).exists()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            return file;
        }

        YoutubeVideo video = YoutubeDownloader.getVideo(videoId);
        List<AudioFormat> audioFormats = video.audioFormats();
        AudioFormat audioFormat = audioFormats.stream().max((af, b) -> af.bitrate()).get();
        if (audioFormat.contentLength() >= 15000000) {
            return null;
        }

        videoRequests.add(videoId);

        File file = new File("temp/" + videoId);
        video.download(audioFormat, file);

        File audioFile = file.listFiles()[0];
        File destinationFile = new File("temp/" + videoId + ".mp3");
        audioFile.renameTo(destinationFile);
        new File("temp/" + videoId).delete();

        return destinationFile;
    }

}
