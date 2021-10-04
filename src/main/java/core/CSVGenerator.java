package core;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVGenerator {

    public static InputStream generateInputStream(List<String[]> rows) {
        String string = generateString(rows);
        return new ByteArrayInputStream(string.getBytes());
    }

    public static String generateString(List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            sb.append(convertLineToCSV(row)).append("\n");
        }
        return sb.toString();
    }

    private static String convertLineToCSV(String[] data) {
        return Stream.of(data)
                .map(CSVGenerator::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    private static String escapeSpecialCharacters(String data) {
        data = data.replace("\r", "");
        if (data.contains(",") || data.contains("\"") || data.contains("'") || data.contains("\n")) {
            data = data.replace("\"", "\"\"");
            data = "\"" + data + "\"";
        }
        return data;
    }

}
