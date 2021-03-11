package core;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import constants.Emojis;
import constants.Locales;
import core.utils.StringUtil;

public class TextManager {

    public static String COMMANDS = "commands", GENERAL = "general", PERMISSIONS = "permissions", VERSIONS = "versions", FAQ = "faq";

    private static final HashMap<String, ResourceBundle> bundles = new HashMap<>();

    public static String getString(Locale locale, String category, String key, String... args) {
        return getString(locale, category, key, -1, args);
    }

    public static String getString(Locale locale, String category, String key, int option, String... args) {
        ResourceBundle texts = bundles.computeIfAbsent(category, k -> ResourceBundle.getBundle(category, locale, new UTF8Control()));

        if (!texts.containsKey(key)) {
            MainLogger.get().error("Key " + key + " not found in " + category + " and thread " + Thread.currentThread().getName());
            return "???";
        } else {
            //Get String
            String text = texts.getString(key);

            //Calculate References
            for (String reference : StringUtil.extractGroups(text, "%<", ">")) {
                String newString;
                if (reference.contains(".")) {
                    String[] argsLink = reference.split("\\.");
                    newString = getString(locale, argsLink[0], argsLink[1]);
                } else {
                    newString = getString(locale, category, reference);
                }
                text = text.replace("%<" + reference + ">", newString);
            }

            // calculate multi option element
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
            text = text.replace("%CURRENCY", Emojis.CURRENCY);
            text = text.replace("%COINS", Emojis.COINS);
            text = text.replace("%GROWTH", Emojis.GROWTH);
            text = text.replace("%DAILYSTREAK", Emojis.DAILY_STREAK);

            return text;
        }
    }

    public static String getString(Locale locale, String category, String key, boolean secondOption, String... args) {
        if (!secondOption) {
            return getString(locale, category, key, 0, args);
        } else {
            return getString(locale, category, key, 1, args);
        }
    }

    public static String getNoResultsString(Locale locale, String content) {
        return TextManager.getString(locale, TextManager.GENERAL, "no_results_description", StringUtil.shortenString(content, 32));
    }

    public static int getKeySize(String category) {
        ResourceBundle texts = ResourceBundle.getBundle(category, new Locale(Locales.EN));
        return texts.keySet().size();
    }

}
