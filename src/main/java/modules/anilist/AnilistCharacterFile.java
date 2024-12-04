package modules.anilist;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import core.LocalFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class AnilistCharacterFile {

    public static final File FILE = new LocalFile(LocalFile.Directory.RESOURCES, "anilist_characters.jsonl");

    public static void collect() throws ExecutionException, InterruptedException, IOException {
        try (PrintWriter out = new PrintWriter(FILE)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            for (int i = 0; i < 200; i++) {
                List<AnilistCharacter> newCharacters = AnilistDownloader.getCharacters(i);
                for (AnilistCharacter newCharacter : newCharacters) {
                    out.println(mapper.writeValueAsString(newCharacter));
                }
                System.out.println("Page " + i + " collected...");
                TimeUnit.SECONDS.sleep(4);
            }
        }
    }

    public static List<AnilistCharacter> getCharacters() throws IOException {
        ArrayList<AnilistCharacter> characters = new ArrayList<>();
        List<String> lines = Files.readAllLines(FILE.toPath());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (String line : lines) {
            AnilistCharacter anilistCharacter = mapper.readValue(line, AnilistCharacter.class);
            characters.add(anilistCharacter);
        }
        return characters;
    }

}
