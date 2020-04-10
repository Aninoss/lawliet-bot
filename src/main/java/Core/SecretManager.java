package Core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class SecretManager {

    public static String getString(String key) throws IOException {
        ResourceBundle texts = PropertyResourceBundle.getBundle("secrets");
        if (texts.containsKey(key)) {
            return texts.getString(key);
        } else {
            throw new IOException("Key " + key + " not found!");
        }
    }

}
