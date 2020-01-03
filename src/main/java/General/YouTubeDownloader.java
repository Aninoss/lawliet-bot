package General;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.AudioFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class YouTubeDownloader {

    private static ArrayList<String> videoRequests = new ArrayList<>();

    public static Optional<String> getVideoID(String str) {
        String[] starters = {"v=", "youtu.be/"};
        for(String starter: starters) {
            if (str.contains(starter)) {
                int index = str.indexOf(starter) + starter.length();
                if (index + 11 > str.length()) return Optional.empty();
                str = str.substring(index, index + 11);

                for(char c: str.toCharArray()) {
                    if (!Character.isLetter(c) && !Character.isDigit(c) && c != '-' && c != '_'){
                        return Optional.empty();
                    }
                }

                try {
                    YoutubeVideo video = YoutubeDownloader.getVideo(str);
                    List<AudioFormat> audioFormats = video.audioFormats();
                    if (audioFormats.size() == 0) return Optional.empty();
                    AudioFormat audioFormat = audioFormats.stream().max((af, b) -> af.bitrate()).get();
                    if (audioFormat.contentLength() >= 15000000) return Optional.of("%toolong");
                    return Optional.of(str);
                } catch (IOException | YoutubeException e) {
                    //Ignore
                    e.printStackTrace();
                    return Optional.of("%error");
                }
            }
        }

        return Optional.empty();
    }

    public static File downloadAudio(String videoId) throws IOException, YoutubeException, InterruptedException {
        while (videoRequests.contains(videoId)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        File file;
        if ((file = new File("temp/" + videoId + ".mp3")).exists()) {
            return file;
        }

        videoRequests.add(videoId);

        try {
            ProcessBuilder pb = new ProcessBuilder("./ytmp3.sh", videoId);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();

            return new File("temp/" + videoId + ".mp3");
        } finally {
            videoRequests.remove(videoId);
        }
    }

}
