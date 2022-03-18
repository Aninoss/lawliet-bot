package commands;

import java.util.*;
import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import core.MainLogger;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.checkerframework.checker.nullness.qual.NonNull;
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

    public static SlashMeta process(SlashCommandInteractionEvent event) {
        SlashAdapter slashAdapter = slashAdapterMap.get(event.getName());
        if (slashAdapter != null) {
            return slashAdapter.process(event);
        } else {
            return null;
        }
    }

    @NonNull
    public static List<Command.Choice> retrieveChoices(CommandAutoCompleteInteractionEvent event) {
        SlashAdapter slashAdapter = slashAdapterMap.get(event.getName());
        if (slashAdapter != null) {
            List<Command.Choice> choiceList = slashAdapter.retrieveChoices(event);
            return choiceList.subList(0, Math.min(25, choiceList.size()));
        } else {
            return Collections.emptyList();
        }
    }

    private static void insert(SlashAdapter adapter) {
        slashAdapterMap.put(adapter.name(), adapter);
    }

}
