package General;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    public static List<String> readInList(File file) throws IOException {
        ArrayList<String> list = new ArrayList<>();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(file), StandardCharsets.UTF_8));

        String line = "";
        while((line = br.readLine()) != null) {
            list.add(line);
        }

        return list;
    }
}
