package modules.translate;

import commands.Category;
import constants.Language;
import core.TextManager;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TranslateMessageInteractionManager {

    public static List<CommandData> generateMessageCommands() {
        CommandData commandData = Commands.message(TextManager.getString(Language.EN.getLocale(), Category.AI_TOYS, "translate_interaction"));
        commandData.setGuildOnly(true);
        Arrays.stream(Language.values())
                .filter(language -> language != Language.EN)
                .forEach(language -> {
                    String name = TextManager.getString(language.getLocale(), Category.AI_TOYS, "translate_interaction");
                    commandData.setNameLocalization(language.getDiscordLocales()[0], name);
                });

        return Collections.singletonList(commandData);
    }

}
