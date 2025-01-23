package commands.runnables.casinocategory;

import commands.*;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import commands.listeners.OnStringSelectMenuListener;
import commands.runnables.CasinoAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import mysql.modules.casinostats.CasinoStatsData;
import mysql.modules.casinostats.DBCasinoStats;
import mysql.modules.casinotracking.DBCasinoTracking;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "casinostats",
        emoji = "📈",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class CasinoStatsCommand extends Command implements OnButtonListener, OnStringSelectMenuListener {

    public static final String BUTTON_ID_ENABLE = "enable";
    public static final String BUTTON_ID_DISABLE = "disable";
    public static final String BUTTON_ID_RESET = "reset";
    public static final String SELECT_MENU_ALLGAMES_ID = "all";

    private List<Command> casinoCommands;
    private String selectedGame = SELECT_MENU_ALLGAMES_ID;
    private boolean stopLock = true;

    public CasinoStatsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        this.casinoCommands = CommandContainer.getFullCommandList().stream()
                .map(clazz -> CommandManager.createCommandByClass(clazz, getLocale(), getPrefix()))
                .filter(CasinoStatsCommand::commandIsValid)
                .peek(command -> {
                    if (command.getTrigger().equalsIgnoreCase(args) ||
                            Arrays.stream(command.getCommandProperties().aliases()).anyMatch(a -> a.equalsIgnoreCase(args))
                    ) {
                        selectedGame = command.getTrigger();
                    }
                })
                .collect(Collectors.toList());

        if (!args.isEmpty() && !args.equalsIgnoreCase("all") && selectedGame.equals(SELECT_MENU_ALLGAMES_ID)) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
        }

        registerButtonListener(event.getMember(), false);
        registerStringSelectMenuListener(event.getMember());
        return true;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        switch (event.getComponentId()) {
            case BUTTON_ID_ENABLE -> {
                DBCasinoTracking.getInstance().retrieve().setActive(event.getMember().getIdLong(), true);
                setLog(LogStatus.SUCCESS, getString("settracking", true));
                stopLock = true;
            }
            case BUTTON_ID_DISABLE -> {
                DBCasinoTracking.getInstance().retrieve().setActive(event.getMember().getIdLong(), false);
                setLog(LogStatus.SUCCESS, getString("settracking", false));
                stopLock = true;
            }
            case BUTTON_ID_RESET -> {
                if (stopLock) {
                    setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                    stopLock = false;
                } else {
                    DBCasinoStats.getInstance().removeMember(event.getGuild().getIdLong(), event.getMember().getIdLong());
                    setLog(LogStatus.SUCCESS, getString("setreset"));
                    stopLock = true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onStringSelectMenu(@NotNull StringSelectInteractionEvent event) throws Throwable {
        this.selectedGame = event.getValues().get(0);
        return true;
    }

    @Nullable
    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        CasinoStatsData statsData = DBCasinoStats.getInstance().retrieve(new DBCasinoStats.Key(member.getGuild().getIdLong(), member.getIdLong()));
        boolean enabled = DBCasinoTracking.getInstance().retrieve().isActive(member.getIdLong());
        boolean hasData = statsData.hasData();

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        ArrayList<ActionRow> actionRows = new ArrayList<>();
        if (enabled || hasData) {
            String gameName = selectedGame.equals(SELECT_MENU_ALLGAMES_ID)
                    ? getString("allgames")
                    : TextManager.getString(getLocale(), Category.CASINO, selectedGame + "_title");
            String description = getString(
                    "data",
                    StringUtil.numToString(statsData.getGames(selectedGame)),
                    StringUtil.numToString(statsData.getGamesWon(selectedGame)),
                    StringUtil.numToString(statsData.getGamesLost(selectedGame)),
                    StringUtil.numToString(statsData.getWinRatePercent(selectedGame)),
                    Emojis.COINS.getFormatted(),
                    StringUtil.numToString(statsData.getCoinsWon(selectedGame)),
                    StringUtil.numToString(statsData.getCoinsLost(selectedGame)),
                    StringUtil.numToString(statsData.getAverageCoinsPerGame(selectedGame))
            );
            eb.setDescription(getString("tracking", StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), enabled)))
                    .addField(gameName, description, false);
            actionRows.add(ActionRow.of(generateSelectMenu()));
        } else {
            eb.setDescription(getString("disabled"));
        }

        Button stateButton = Button.of(ButtonStyle.PRIMARY, enabled ? BUTTON_ID_DISABLE : BUTTON_ID_ENABLE, getString("button_state", !enabled));
        if (hasData) {
            Button resetButton = Button.of(ButtonStyle.DANGER, BUTTON_ID_RESET, getString("button_reset"));
            actionRows.add(ActionRow.of(stateButton, resetButton));
        } else {
            actionRows.add(ActionRow.of(stateButton));
        }
        setActionRows(actionRows);
        return eb;
    }

    public static boolean commandIsValid(Command command) {
        return command.getCategory() == Category.CASINO &&
                !(command instanceof CasinoStatsCommand) &&
                (!(command instanceof CasinoAbstract) || ((CasinoAbstract) command).allowBet());
    }

    private SelectMenu generateSelectMenu() {
        StringSelectMenu.Builder menuBuilder = StringSelectMenu.create("game")
                .setMinValues(1)
                .addOption(getString("allgames"), SELECT_MENU_ALLGAMES_ID);
        for (Command command : casinoCommands) {
            menuBuilder.addOption(
                    command.getCommandLanguage().getTitle(),
                    command.getTrigger()
            );
        }
        return menuBuilder.setDefaultValues(List.of(selectedGame))
                .build();
    }

}
