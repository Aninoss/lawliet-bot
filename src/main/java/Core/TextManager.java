package Core;

import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.Language;
import Constants.Locales;
import Constants.Settings;
import Core.Utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Locale;
import java.util.ResourceBundle;

public class TextManager {

    final static Logger LOGGER = LoggerFactory.getLogger(TextManager.class);
    private static final String EMPTY_STRING = "???";

    public static String COMMANDS = "commands", GENERAL = "general", PERMISSIONS = "permissions", ANSWERS = "answers", VERSIONS = "versions", FAQ = "faq";


    public static String getString(Command command, String key, String... args) {
        return getString(command, key, -1, args);
    }

    public static String getString(Command command, String key, boolean secondOption, String... args) {
        return getString(command, key, secondOption ? 1 : 0, args);
    }

    public static String getString(Command command, String key, int option, String... args) {
        String bundle = String.format("commands/%s/%s", command.getCategory(), command.getTrigger());
        String str = getString(command.getLocale(), bundle, key, option, args);
        if (command.getPrefix() != null) str = str.replace("%PREFIX", command.getPrefix());
        return str;
    }


    public static String getString(Locale locale, Command command, String key, String... args) {
        return getString(locale, command, key, -1, args);
    }

    public static String getString(Locale locale, Command command, String key, boolean secondOption, String... args) {
        return getString(locale, command, key, secondOption ? 1 : 0, args);
    }

    public static String getString(Locale locale, Command command, String key, int option, String... args) {
        String bundle = String.format("commands/%s/%s", command.getCategory(), command.getTrigger());
        String str = getString(locale, bundle, key, option, args);
        if (command.getPrefix() != null) str = str.replace("%PREFIX", command.getPrefix());
        return str;
    }


    public static String getString(Locale locale, String category, String key, String... args) {
        return getString(locale, category, key, -1, args);
    }

    public static String getString(Locale locale, String category, String key, boolean secondOption, String... args) {
        return getString(locale, category, key, secondOption ? 1 : 0, args);
    }

    public static String getString(Locale locale, String category, String key, int option, String... args) {
        ResourceBundle texts = ResourceBundle.getBundle(category, locale, new UTF8Control());
        if (!texts.containsKey(key)) {
            LOGGER.error("Key " + key + " not found in " + category + " and thread " + Thread.currentThread().getName());
            if (StringUtil.getLanguage(locale) != Language.EN) return getString(new Locale(Locales.EN), category, key, option, args);
            return EMPTY_STRING;
        } else {
            //Get String
            String text = texts.getString(key);

            //Calculate References
            for(String reference: StringUtil.extractGroups(text, "%<", ">")) {
                String newString;
                if (reference.contains(".")) {
                    String[] argsLink = reference.split("\\.");
                    newString = getString(locale, argsLink[0], argsLink[1]);
                    if (newString.equals(EMPTY_STRING) && argsLink[0].equals(COMMANDS) && argsLink[1].contains("_")) {
                        String commandTrigger = argsLink[1].split("_")[0];
                        Class<? extends Command> clazz = CommandContainer.getInstance().getCommands().get(commandTrigger);
                        if (clazz != null) {
                            try {
                                Command command = CommandManager.createCommandByClass(clazz, locale);
                                newString = command.getString(argsLink[1].substring(command.getTrigger().length() + 1));
                            } catch (IllegalAccessException | InstantiationException e) {
                                LOGGER.error("Error while creating command", e);
                            }
                        }
                    }
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
                        if (option <= options.length - 1) text = text.replace("%[" + subText + "]%", options[option]);
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
            text = text.replace("%CURRENCY", Settings.CURRENCY)
                    .replace("%COINS", Settings.COINS)
                    .replace("%GROWTH", Settings.GROWTH)
                    .replace("%DAILYSTREAK", Settings.DAILY_STREAK);

            return text;
        }
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
