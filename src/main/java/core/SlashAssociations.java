package core;

import java.util.HashMap;
import java.util.List;
import commands.Command;
import commands.SlashCommandManager;

public class SlashAssociations {

    private static final HashMap<String, String> SlashToMessageAssociationMap = SlashCommandManager.generateSlashAssociationMap();
    private static final HashMap<Long, String> idToNameAssociationMap = new HashMap<>();

    public static void registerSlashCommands(List<net.dv8tion.jda.api.interactions.commands.Command> commands) {
        for (net.dv8tion.jda.api.interactions.commands.Command command : commands) {
            idToNameAssociationMap.put(command.getIdLong(), command.getName());
        }
    }

    public static String findName(Class<? extends Command> clazz) {
        String trigger = Command.getCommandProperties(clazz).trigger();
        String categoryId = Command.getCategory(clazz).getId();

        if (SlashToMessageAssociationMap.containsKey(trigger)) {
            return SlashToMessageAssociationMap.get(trigger);
        } else {
            return SlashToMessageAssociationMap.getOrDefault(categoryId, null);
        }
    }

    public static String getNameFromId(long id) {
        return idToNameAssociationMap.get(id);
    }

}
