package General;

import Constants.Settings;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

public class TextManager {

    public static String COMMANDS = "commands", GENERAL = "general", PERMISSIONS = "permissions", ANSWERS = "answers", VERSIONS = "versions";

    public static String getString(Locale locale, String category, String key, String... args) {
        return getString(locale, category, key, -1, args);
    }

    public static String getString(Locale locale, String category, String key, int option, String... args) {
        ResourceBundle texts = ResourceBundle.getBundle(category, locale, new UTF8Control());
        if (!texts.containsKey(key)) {
            ExceptionHandler.showErrorLog("Key " + key + " not found in " + category);
            return "???";
        } else {
            //Get String
            String text = texts.getString(key);

            //Calculate References
            while (text.contains("%<")) {
                String replaceString = Tools.cutString(text, "%<", ">");
                String[] argsLink = replaceString.split("\\.");
                String newString = getString(locale, argsLink[0], argsLink[1]);
                text = text.replace("%<" + replaceString + ">", newString);
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
            for (int i = args.length-1; i >= 0; i--) {
                text = text.replace("%" + i, args[i]);
            }

            //Fill global variables
            text = text.replace("%CURRENCY", Settings.CURRENCY);
            text = text.replace("%COINS", Settings.COINS);
            text = text.replace("%GROWTH", Settings.GROWTH);

            return text;
        }
    }

    public static String getString(Locale locale, String category, String key, boolean secondOption, String... args) throws IOException {
        if (!secondOption) return getString(locale, category, key, 0, args);
        else return getString(locale, category, key, 1, args);
    }

    public static int getKeySize(Locale locale, String category) {
        ResourceBundle texts = ResourceBundle.getBundle(category, locale);
        return texts.keySet().size();
    }
}
