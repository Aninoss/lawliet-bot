package core;

import java.util.HashMap;
import java.util.NoSuchElementException;
import commands.Command;
import commands.SlashCommandManager;

public class SlashAssociations {

    private static final HashMap<String, String> associationMap = SlashCommandManager.generateSlashAssociationMap();

    public static String find(Command command) {
        if (associationMap.containsKey(command.getTrigger())) {
            return associationMap.get(command.getTrigger());
        } else if (associationMap.containsKey(command.getCategory().getId())) {
            return associationMap.get(command.getCategory().getId());
        } else {
            throw new NoSuchElementException("No such association key");
        }
    }

}
