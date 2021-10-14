package commands;

import java.util.*;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import core.MainLogger;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.reflections.Reflections;

public class SlashCommandManager {

    private static final HashMap<String, SlashAdapter> slashAdapterMap = new HashMap<>();

    static {
        Reflections reflections = new Reflections("commands/slashadapters/adapters");
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(Slash.class);
        annotated.stream()
                .map(clazz -> {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        MainLogger.get().error("Error when creating slash adapter class", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(obj -> obj instanceof SlashAdapter)
                .map(obj -> (SlashAdapter) obj)
                .forEach(SlashCommandManager::insert);
    }

    public static List<CommandData> initialize() {
        ArrayList<CommandData> commandDataList = new ArrayList<>();
        for (SlashAdapter slashAdapter : slashAdapterMap.values()) {
            commandDataList.add(slashAdapter.generateCommandData());
        }
        return commandDataList;
    }

    public static SlashMeta process(SlashCommandEvent event) {
        SlashAdapter slashAdapter = slashAdapterMap.get(event.getName());
        if (slashAdapter != null) {
            return slashAdapter.process(event);
        } else {
            return null;
        }
    }

    private static void insert(SlashAdapter adapter) {
        slashAdapterMap.put(adapter.name(), adapter);
    }

}
