package commands;

import commands.slashadapters.Slash;
import commands.slashadapters.SlashAdapter;
import commands.slashadapters.SlashMeta;
import constants.AssetIds;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import core.SlashAssociations;
import modules.moduserinteractions.ModUserInteractionManager;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.reflections.Reflections;

import java.util.*;
import java.util.stream.Collectors;

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

    public static void sendCommandUpdate(JDA jda, boolean forceUpdate) {
        try {
            ArrayList<CommandData> commandDataList = initialize();

            RestAction<List<Command>> retrieveCommandsAction;
            CommandListUpdateAction commandListUpdateAction;
            if (Program.productionMode()) {
                retrieveCommandsAction = jda.retrieveCommands();
                commandListUpdateAction = jda.updateCommands();
            } else {
                Guild guild = ShardManager.getLocalGuildById(AssetIds.BETA_SERVER_ID).get();
                retrieveCommandsAction = guild.retrieveCommands();
                commandListUpdateAction = guild.updateCommands();
            }

            retrieveCommandsAction.queue(commands -> {
                if (forceUpdate || commandsHaveChanged(commands, commandDataList)) {
                    MainLogger.get().info("Pushing new slash commands ({})", commandDataList.size());
                    commandListUpdateAction.addCommands(commandDataList)
                            .queue(SlashAssociations::registerSlashCommands);
                } else {
                    MainLogger.get().info("No new slash commands found");
                    SlashAssociations.registerSlashCommands(commands);
                }
            });
        } catch (Throwable e) {
            MainLogger.get().error("Exception on slash commands load", e);
        }
    }

    public static SlashMeta process(SlashCommandInteractionEvent event, GuildEntity guildEntity) {
        SlashAdapter slashAdapter = slashAdapterMap.get(event.getName());
        if (slashAdapter != null) {
            return slashAdapter.process(event, guildEntity);
        } else {
            return null;
        }
    }

    public static HashMap<String, String> generateSlashAssociationMap() {
        HashMap<String, String> associationMap = new HashMap<>();
        for (SlashAdapter slashAdapter : slashAdapterMap.values()) {
            for (String messageCommandAssociation : slashAdapter.messageCommandAssociations()) {
                associationMap.put(messageCommandAssociation, slashAdapter.name());
            }
        }
        return associationMap;
    }

    @NonNull
    public static List<Command.Choice> retrieveChoices(CommandAutoCompleteInteractionEvent event) {
        SlashAdapter slashAdapter = slashAdapterMap.get(event.getName());
        if (slashAdapter != null) {
            List<Command.Choice> choiceList = slashAdapter.retrieveChoices(event);
            return choiceList.subList(0, Math.min(25, choiceList.size()));
        }
        return Collections.emptyList();
    }

    private static void insert(SlashAdapter adapter) {
        slashAdapterMap.put(adapter.name(), adapter);
    }

    private static boolean commandsHaveChanged(List<Command> previousCommands, List<CommandData> newCommands) {
        HashSet<Integer> previousCommandHashesSet = previousCommands.stream()
                .map(command -> commandDataHashCode(CommandData.fromCommand(command)))
                .collect(Collectors.toCollection(HashSet::new));
        HashSet<Integer> newCommandHashesSet = newCommands.stream()
                .map(SlashCommandManager::commandDataHashCode)
                .collect(Collectors.toCollection(HashSet::new));

        return newCommandHashesSet.stream().anyMatch(commandData -> !previousCommandHashesSet.contains(commandData)) ||
                previousCommandHashesSet.stream().anyMatch(commandData -> !newCommandHashesSet.contains(commandData));
    }

    private static ArrayList<CommandData> initialize() {
        ArrayList<CommandData> commandDataList = new ArrayList<>();
        for (SlashAdapter slashAdapter : slashAdapterMap.values()) {
            if (!slashAdapter.onlyPublicVersion() || Program.publicVersion()) {
                commandDataList.add(slashAdapter.generateCommandData());
            }
        }
        commandDataList.addAll(ModUserInteractionManager.generateUserCommands());
        return commandDataList;
    }

    private static int commandDataHashCode(CommandData commandData) {
        if (commandData instanceof SlashCommandData) {
            SlashCommandData slashCommandData = (SlashCommandData) commandData;
            return Objects.hash(
                    slashCommandData.getName(),
                    slashCommandData.getDescription(),
                    slashCommandData.getDefaultPermissions().getPermissionsRaw(),
                    slashCommandData.isNSFW(),
                    optionDataListHashCode(slashCommandData.getOptions()),
                    subcommandGroupDataListHashCode(slashCommandData.getSubcommandGroups()),
                    subcommandDataListHashCode(slashCommandData.getSubcommands())
            );
        } else {
            return Objects.hash(
                    commandData.getName(),
                    commandData.getDefaultPermissions().getPermissionsRaw(),
                    commandData.getType(),
                    commandData.isNSFW()
            );
        }
    }

    private static int optionDataListHashCode(List<OptionData> options) {
        return options.stream()
                .map(optionData -> Objects.hash(
                        optionData.getName(),
                        optionData.getType(),
                        optionData.getDescription(),
                        optionData.getChannelTypes(),
                        optionData.getMaxLength(),
                        optionData.getMaxValue(),
                        optionData.getMinLength(),
                        optionData.getMinValue(),
                        optionData.getChoices(),
                        optionData.isRequired(),
                        optionData.isAutoComplete()
                ))
                .collect(Collectors.toList())
                .hashCode();
    }
    
    private static int subcommandGroupDataListHashCode(List<SubcommandGroupData> subcommandGroups) {
        return subcommandGroups.stream()
                .map(group -> Objects.hash(
                        group.getName(),
                        group.getDescription(),
                        subcommandDataListHashCode(group.getSubcommands())
                ))
                .collect(Collectors.toList())
                .hashCode();
    }

    private static int subcommandDataListHashCode(List<SubcommandData> subcommands) {
        return subcommands.stream()
                .map(subcommand -> Objects.hash(
                        subcommand.getName(),
                        subcommand.getDescription(),
                        optionDataListHashCode(subcommand.getOptions())
                        ))
                .collect(Collectors.toList())
                .hashCode();
    }

}
