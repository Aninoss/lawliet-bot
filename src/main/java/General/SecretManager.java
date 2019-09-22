package General;

import Constants.Settings;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class SecretManager {
    public static String getString(String key) throws IOException {
        File file = new File("src/main/resources/secrets.properties");
        if (Bot.isDebug() && !file.exists()) createPropertiesFile(file);

        ResourceBundle texts = PropertyResourceBundle.getBundle("secrets");
        if (!texts.containsKey(key)) {
            throw new IOException("Key " + key + " not found!");
        } else {
            String text = texts.getString(key);
            return text;
        }
    }

    private static void createPropertiesFile(File file) {
        String[] propertyKeys = {
                "bot.token.debugger",
                "bot.token",
                "database.ip",
                "database.username",
                "database.password",
                "discordbots.token",
                "discordbots.auth",
                "donation.auth",
                "SIGNALTRANSMITTER.username",
                "SIGNALTRANSMITTER.password",
                "deepai.toke"};

        try {
            FileWriter fileWriter = new FileWriter(file, false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for(String str: propertyKeys)
                bufferedWriter.write(str + " =\r\n");
            bufferedWriter.flush();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("### THE BOT HAS CREATED THE SECRETS FILE \"src/main/resources/secrets.properties\" ###");
    }
}
