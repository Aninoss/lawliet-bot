package commands.runnables.informationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ListAbstract;
import constants.Emojis;
import core.TextManager;
import core.utils.MentionUtil;
import javafx.util.Pair;
import modules.BotLogs;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "botlogs",
        userGuildPermissions = Permission.VIEW_AUDIT_LOGS,
        emoji = "🔖",
        executableWithoutArgs = true
)
public class BotLogsCommand extends ListAbstract {

    private static final String SELECT_MENU_ID_EXPAND = "expand";

    private List<UUID> slots;
    private final ArrayList<Integer> expandableEntries = new ArrayList<>();
    private int expandedEntry = -1;

    public BotLogsCommand(Locale locale, String prefix) {
        super(locale, prefix, 5);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        registerList(event.getMember(), args);
        return true;
    }

    @Override
    public boolean onStringSelectMenu(@NotNull StringSelectInteractionEvent event) throws Throwable {
        if (event.getComponentId().equals(SELECT_MENU_ID_EXPAND)) {
            expandedEntry = event.getValues().isEmpty() ? -1 : Integer.parseInt(event.getValues().get(0));
            return true;
        }

        return super.onStringSelectMenu(event);
    }

    @Override
    protected int configure(Member member, int orderBy) throws Throwable {
        slots = BotLogEntity.findAll(getEntityManager(), member.getGuild().getIdLong()).stream()
                .map(BotLogEntity::getId)
                .collect(Collectors.toList());
        return slots.size();
    }

    @Override
    protected Pair<String, String> getEntry(Member member, int i, int orderBy) {
        BotLogEntity botLog = getEntityManager().find(BotLogEntity.class, slots.get(i));
        if (botLog == null) {
            String deleted = getString("deleted");
            return new Pair<>(deleted, "> " + deleted);
        }

        String memberString = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), List.of(member.getUser())).getMentionText();
        String targetedUserString = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), botLog.getTargetedUserList()).getMentionText();

        StringBuilder title = new StringBuilder();
        if (botLog.getEvent().getValuesRelationship() != BotLogEntity.ValuesRelationship.EMPTY) {
            title.append(Emojis.LETTERS[expandableEntries.size()].getFormatted())
                    .append("｜");
            expandableEntries.add(i);
            if (expandedEntry == i) {
                addExpandedValueFields(botLog);
            }
        }

        if (botLog.getTimestampUpdate() == null) {
            title.append(TimeFormat.DATE_TIME_SHORT.atInstant(botLog.getTimeCreate()).toString());
        } else {
            title.append(TimeFormat.DATE_TIME_SHORT.atInstant(botLog.getTimeCreate()))
                    .append(" - ")
                    .append(TimeFormat.DATE_TIME_SHORT.atInstant(botLog.getTimeUpdate()));
        }
        String message = TextManager.getString(getLocale(), TextManager.LOGS, "event_" + botLog.getEvent().name(), memberString, targetedUserString);
        return new Pair<>(title.toString(), getString("slot", message));
    }

    @Override
    protected List<ActionRow> postProcessAddActionRows() {
        if (expandableEntries.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<SelectOption> selectOptions = new ArrayList<>();
        for (int i = 0; i < expandableEntries.size(); i++) {
            int entryI = expandableEntries.get(i);
            char c = (char) ('A' + i);
            SelectOption selectOption = SelectOption.of(getString("expand_option", String.valueOf(c)), String.valueOf(entryI))
                    .withDefault(entryI == expandedEntry)
                    .withEmoji(Emojis.LETTERS[i]);
            selectOptions.add(selectOption);
        }

        StringSelectMenu selectMenu = StringSelectMenu.create(SELECT_MENU_ID_EXPAND)
                .addOptions(selectOptions)
                .setRequiredRange(0, 1)
                .setPlaceholder(getString("expand_placeholder"))
                .build();

        expandableEntries.clear();
        return List.of(ActionRow.of(selectMenu));
    }

    @Override
    protected void postProcessEmbed(EmbedBuilder eb, int orderBy) {
        eb.setDescription(getString("desc") + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted());
    }

    private void addExpandedValueFields(BotLogEntity botLog) {
        BotLogs.getExpandedValueFields(getLocale(), getGuildId().get(), botLog, true)
                .forEach(p -> addEmbedField(p.getKey(), p.getValue(), true));
    }

}