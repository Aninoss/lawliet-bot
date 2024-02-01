package modules;

import commands.Category;
import constants.Emojis;
import core.TextManager;
import core.atomicassets.AtomicGuildChannel;
import core.atomicassets.AtomicMember;
import core.atomicassets.AtomicRole;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import javafx.util.Pair;
import mysql.hibernate.entity.BotLogEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BotLogs {

    public static List<Pair<String, String>> getExpandedValueFields(Locale locale, long guildId, BotLogEntity botLog, boolean inBlocks) {
        BotLogEntity.ValueType valueType = botLog.getEvent().getValueType();
        ArrayList<Pair<String, String>> fields = new ArrayList<>();

        switch (botLog.getEvent().getValuesRelationship()) {
            case EMPTY -> {
            }
            case OLD_AND_NEW -> {
                if (!botLog.getValues0().isEmpty()) {
                    String values = extractValues(locale, guildId, botLog.getValues0(), valueType, inBlocks);
                    fields.add(new Pair<>(TextManager.getString(locale, TextManager.LOGS, "values_old"), values));
                }
                String values = botLog.getValues1().isEmpty()
                        ? TextManager.getString(locale, TextManager.GENERAL, "empty")
                        : extractValues(locale, guildId, botLog.getValues1(), valueType, inBlocks);
                fields.add(new Pair<>(TextManager.getString(locale, TextManager.LOGS, "values_new"), values));
            }
            case ADD_AND_REMOVE -> {
                if (!botLog.getValues0().isEmpty()) {
                    String values = extractValues(locale, guildId, botLog.getValues0(), valueType, inBlocks);
                    fields.add(new Pair<>(TextManager.getString(locale, TextManager.LOGS, "values_added"), values));
                }
                if (!botLog.getValues1().isEmpty()) {
                    String values = extractValues(locale, guildId, botLog.getValues1(), valueType, inBlocks);
                    fields.add(new Pair<>(TextManager.getString(locale, TextManager.LOGS, "values_removed"), values));
                }
            }
            case SINGLE_VALUE_COLUMN -> {
                if (!botLog.getValues0().isEmpty()) {
                    String values = extractValues(locale, guildId, botLog.getValues0(), valueType, inBlocks);
                    fields.add(new Pair<>(TextManager.getString(locale, TextManager.LOGS, "values_single"), values));
                }
            }
        }

        return fields;
    }

    private static String extractValues(Locale locale, long guildId, List<String> values, BotLogEntity.ValueType valueType, boolean inBlocks) {
        StringBuilder sb = new StringBuilder();

        for (String value : values) {
            String extracted = switch (valueType) {
                case INTEGER -> StringUtil.numToString(Integer.parseInt(value));
                case DOUBLE -> StringUtil.doubleToString(Double.parseDouble(value), 2, locale);
                case BOOLEAN -> TextManager.getString(locale, TextManager.GENERAL, "onoff", Boolean.parseBoolean(value));
                case STRING -> value;
                case TEXT_KEY -> TextManager.getString(locale, TextManager.LOGS, "values_text_" + value);
                case COMMAND_CATEGORY -> {
                    Category category = Category.fromId(value);
                    if (category != null) {
                        value = TextManager.getString(locale, TextManager.COMMANDS, value);
                    }
                    if (inBlocks) {
                        value = "`" + value + "`";
                    }
                    yield value;
                }
                case DURATION -> TimeUtil.getRemainingTimeString(locale, Long.parseLong(value) * 60_000, false);
                case CHANNEL -> {
                    AtomicGuildChannel atomicGuildChannel = new AtomicGuildChannel(guildId, Long.parseLong(value));
                    if (inBlocks) {
                        yield atomicGuildChannel.getPrefixedNameInField(locale);
                    } else {
                        yield atomicGuildChannel.getPrefixedName(locale);
                    }
                }
                case ROLE -> {
                    AtomicRole atomicRole = new AtomicRole(guildId, Long.parseLong(value));
                    if (inBlocks) {
                        yield atomicRole.getPrefixedNameInField(locale);
                    } else {
                        yield atomicRole.getPrefixedName(locale);
                    }
                }
                case MEMBER -> {
                    AtomicMember atomicMember = new AtomicMember(guildId, Long.parseLong(value));
                    if (inBlocks) {
                        yield atomicMember.getPrefixedNameInField(locale);
                    } else {
                        yield atomicMember.getPrefixedName(locale);
                    }
                }
            };
            sb.append("- ")
                    .append(extracted)
                    .append("\n");
        }

        sb.append(Emojis.ZERO_WIDTH_SPACE.getFormatted());
        return sb.toString();
    }
    
}
