package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class SecretManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(SecretManager.class);

    public static String getString(String key) {
        ResourceBundle texts = PropertyResourceBundle.getBundle("secrets");
        if (texts.containsKey(key)) {
            return texts.getString(key);
        } else {
            LOGGER.error("Key " + key + " for security file not found in thread " + Thread.currentThread().getName());
            return "???";
        }
    }

}
