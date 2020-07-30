package Core;

import Constants.Locales;
import Constants.Settings;
import Core.Utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.ResourceBundle;

public class TextManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(TextManager.class);
    public static String COMMANDS = "commands", GENERAL = "general", PERMISSIONS = "permissions", VERSIONS = "versions", FAQ = "faq";

    public static String getString(Locale locale, String category, String key, String... args) {
        return getString(locale, category, key, -1, args);
    }

    public static String getString(Locale locale, String category, String key, int option, String... args) {
        ResourceBundle texts = ResourceBundle.getBundle(category, locale, new UTF8Control());

        if (!texts.containsKey(key)) {
            LOGGER.error("Key " + key + " not found in " + category + " and thread " + Thread.currentThread().getName());
            return "???";
        } else {
            //Get String
            String text = texts.getString(key);

            //Calculate References
            for(String reference: StringUtil.extractGroups(text, "%<", ">")) {
                String newString;
                if (reference.contains(".")) {
                    String[] argsLink = reference.split("\\.");
                    newString = getString(locale, argsLink[0], argsLink[1]);
                } else {
                    newString = getString(locale, category, reference);
                }
                text = text.replace("%<" + reference + ">", newString);
            }

            //Calculte Multi Option Element
            if (option >= 0) {
                String[] parts = text.split("%\\[");
                for (int i = 1; i < parts.length; i++) {
                    String subText = parts[i];
                    if (subText.contains("]%")) {
                        subText = subText.split("]%")[0];
                        String[] options = subText.split("\\|");
                        text = text.replace("%[" + subText + "]%", options[option]);
                    }
                }
            }

            //Fill Params
            if (args != null) {
                for (int i = args.length - 1; i >= 0; i--) {
                    text = text.replace("%l" + i, args[i].toLowerCase());
                    text = text.replace("%" + i, args[i]);
                }
            }

            //Fill global variables
            text = text.replace("%CURRENCY", Settings.CURRENCY);
            text = text.replace("%COINS", Settings.COINS);
            text = text.replace("%GROWTH", Settings.GROWTH);
            text = text.replace("%DAILYSTREAK", Settings.DAILY_STREAK);

            return text;
        }
    }

    public static String getString(Locale locale, String category, String key, boolean secondOption, String... args) {
        if (!secondOption) return getString(locale, category, key, 0, args);
        else return getString(locale, category, key, 1, args);
    }

    public static int getKeySize(String category) {
        ResourceBundle texts = ResourceBundle.getBundle(category, new Locale(Locales.EN));
        return texts.keySet().size();
    }

    public static int getKeySize(Locale locale, String category) {
        ResourceBundle texts = ResourceBundle.getBundle(category, locale);
        return texts.keySet().size() - 1;
    }
}
