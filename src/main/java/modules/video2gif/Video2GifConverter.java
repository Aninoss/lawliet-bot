package modules.video2gif;

import core.GlobalThreadPool;
import core.LocalFile;
import core.MainLogger;
import core.utils.FileUtil;
import core.utils.RandomUtil;
import net.dv8tion.jda.api.entities.Message;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class Video2GifConverter {

    public static final int MIN_WIDTH = 64;
    public static final int MAX_WIDTH = 720;
    public static final int DEFAULT_WIDTH = 480;
    public static final int MIN_FPS = 1;
    public static final int MAX_FPS = 30;
    public static final int DEFAULT_FPS = 15;
    public static final int MAX_DURATION_SECONDS = 15;
    public static final long MAX_OUTPUT_BYTES = 50L * 1024 * 1024;
    public static final int MAX_CONCURRENT_CONVERSIONS = 2;

    private static final int TIMEOUT_SECONDS = 90;
    private static final int FFMPEG_LOG_MAX_LENGTH = 2000;

    /* The demuxer is forced based on the file extension so that crafted playlist/concat files cannot be processed */
    private static final Map<String, String> DEMUXERS = Map.of(
            "mp4", "mov",
            "m4v", "mov",
            "mov", "mov",
            "webm", "matroska",
            "mkv", "matroska",
            "avi", "avi"
    );

    private static final Semaphore semaphore = new Semaphore(MAX_CONCURRENT_CONVERSIONS);
    private static final Set<Long> activeUsers = ConcurrentHashMap.newKeySet();

    private Video2GifConverter() {
    }

    public static String getSupportedExtension(Message.Attachment attachment) {
        String extension = attachment.getFileExtension();
        if (extension == null) {
            return null;
        }

        extension = extension.toLowerCase();
        return DEMUXERS.containsKey(extension) ? extension : null;
    }

    public static Set<String> getSupportedExtensions() {
        return DEMUXERS.keySet();
    }

    /**
     * Reserves a conversion slot for the user.
     *
     * @return null if the slot was reserved (it must be released with {@link #release(long)}), otherwise the reason it was denied
     */
    public static Failure acquire(long userId) {
        if (!activeUsers.add(userId)) {
            return Failure.USER_BUSY;
        }
        if (!semaphore.tryAcquire()) {
            activeUsers.remove(userId);
            return Failure.SERVER_BUSY;
        }

        return null;
    }

    public static void release(long userId) {
        if (activeUsers.remove(userId)) {
            semaphore.release();
        }
    }

    public static LocalFile convert(Message.Attachment attachment, int width, int fps, boolean loop) throws Video2GifException {
        String extension = getSupportedExtension(attachment);
        if (extension == null) {
            throw new Video2GifException(Failure.UNSUPPORTED_TYPE);
        }

        width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width));
        fps = Math.max(MIN_FPS, Math.min(MAX_FPS, fps));

        String inputName = RandomUtil.generateRandomString(30) + "." + extension;
        String outputName = RandomUtil.generateRandomString(30) + ".gif";
        LocalFile input = new LocalFile(LocalFile.Directory.TEMP, inputName);
        LocalFile output = new LocalFile(LocalFile.Directory.CDN, "temp/" + outputName);

        boolean success = false;
        try {
            try {
                FileUtil.downloadImageAttachment(attachment, input);
            } catch (RuntimeException e) {
                MainLogger.get().warn("Video2gif download failed", e);
                throw new Video2GifException(Failure.DOWNLOAD_FAILED);
            }

            runFfmpeg(DEMUXERS.get(extension), input, output, width, fps, loop);

            if (!output.exists() || output.length() == 0) {
                throw new Video2GifException(Failure.CONVERSION_FAILED);
            }
            if (output.length() > MAX_OUTPUT_BYTES) {
                throw new Video2GifException(Failure.OUTPUT_TOO_LARGE);
            }

            success = true;
            return output;
        } finally {
            FileUtils.deleteQuietly(input);
            if (!success) {
                FileUtils.deleteQuietly(output);
            }
        }
    }

    private static void runFfmpeg(String demuxer, LocalFile input, LocalFile output, int width, int fps, boolean loop) throws Video2GifException {
        String filter = String.format(
                "fps=%d,scale=min(%d\\,iw):-2:flags=lanczos,split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5",
                fps, width
        );

        /* -fs makes ffmpeg stop writing once the output exceeds the limit, so oversized results never fill up the disk */
        List<String> command = List.of(
                new LocalFile("ffmpeg/ffmpeg").getAbsolutePath(),
                "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
                "-protocol_whitelist", "file",
                "-f", demuxer,
                "-t", String.valueOf(MAX_DURATION_SECONDS),
                "-i", input.getAbsolutePath(),
                "-vf", filter,
                "-an", "-sn",
                "-loop", loop ? "0" : "-1",
                "-fs", String.valueOf(MAX_OUTPUT_BYTES + 1),
                "-threads", "2",
                "-filter_threads", "2",
                output.getAbsolutePath()
        );

        Process process;
        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            MainLogger.get().error("Could not start ffmpeg", e);
            throw new Video2GifException(Failure.CONVERSION_FAILED);
        }

        CompletableFuture<String> logFuture = new CompletableFuture<>();
        GlobalThreadPool.submit(() -> {
            try (InputStream inputStream = process.getInputStream()) {
                logFuture.complete(readTail(inputStream));
            } catch (Throwable e) {
                logFuture.complete("");
            }
        });

        try {
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new Video2GifException(Failure.TIMEOUT);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new Video2GifException(Failure.CONVERSION_FAILED);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String log = logFuture.getNow("");
            MainLogger.get().warn("Video2gif ffmpeg exited with code {}: {}", exitCode, log);
            throw new Video2GifException(Failure.CONVERSION_FAILED);
        }
    }

    private static String readTail(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        char[] buffer = new char[1024];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
            if (sb.length() > FFMPEG_LOG_MAX_LENGTH * 2) {
                sb.delete(0, sb.length() - FFMPEG_LOG_MAX_LENGTH);
            }
        }

        return sb.length() > FFMPEG_LOG_MAX_LENGTH ? sb.substring(sb.length() - FFMPEG_LOG_MAX_LENGTH) : sb.toString();
    }


    public enum Failure {

        UNSUPPORTED_TYPE,
        DOWNLOAD_FAILED,
        CONVERSION_FAILED,
        TIMEOUT,
        OUTPUT_TOO_LARGE,
        USER_BUSY,
        SERVER_BUSY

    }


    public static class Video2GifException extends Exception {

        private final Failure failure;

        public Video2GifException(Failure failure) {
            super(failure.name());
            this.failure = failure;
        }

        public Failure getFailure() {
            return failure;
        }

    }

}
