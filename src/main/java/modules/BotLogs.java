package modules;

import commands.Category;
import commands.Command;
import core.TextManager;
import core.atomicassets.AtomicGuildChannel;
import core.atomicassets.AtomicMember;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicUser;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import javafx.util.Pair;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.entities.User;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BotLogs {

    public static String getMessage(Locale locale, BotLogEntity botLog, boolean markdown) {
        List<User> targetedUserList = botLog.getTargetedUserList();
        String memberString = (markdown ? "**" : "") + new AtomicMember(botLog.getGuildId(), botLog.getMemberId()).getUsername(locale) + (markdown ? "**" : "");
        String targetedUserString = MentionUtil.getMentionedStringOfUsernames(locale, targetedUserList).getMentionText();
        if (!markdown) {
            targetedUserString = targetedUserString.replace("**", "");
        }

        String message;
        if (botLog.getEvent().getCommandClass() == null) {
            message = TextManager.getString(locale, TextManager.LOGS, "event_" + botLog.getEvent().name(), memberString, targetedUserString);
        } else {
            String commandTitle = Command.getCommandLanguage(botLog.getEvent().getCommandClass(), locale).getTitle();
            if (botLog.getEvent().getValueNameTextKey() == null) {
                message = TextManager.getString(locale, TextManager.LOGS, targetedUserList.isEmpty() ? "event_command" : "event_command_membertomember", memberString, commandTitle, targetedUserString);
            } else {
                Category category;
                if (botLog.getEvent().getValueNameTextCategory() == null) {
                    category = Command.getCategory(botLog.getEvent().getCommandClass());
                } else {
                    category = botLog.getEvent().getValueNameTextCategory();
                }
                String valueName = TextManager.getString(locale, category, botLog.getEvent().getValueNameTextKey());
                message = TextManager.getString(locale, TextManager.LOGS, targetedUserList.isEmpty() ? "event_command_value" : "event_command_value_membertomember", memberString, valueName, commandTitle, targetedUserString);
            }
        }

        return message;
    }

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
                case DURATION -> TimeUtil.getDurationString(locale, Duration.ofMinutes(Long.parseLong(value)));
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
                case USER -> {
                    AtomicUser atomicUser = AtomicUser.fromOutsideCache(Long.parseLong(value));
                    if (inBlocks) {
                        yield atomicUser.getPrefixedNameInField(locale);
                    } else {
                        yield atomicUser.getPrefixedName(locale);
                    }
                }
            };
            sb.append("- ")
                    .append(extracted)
                    .append("\n");
        }

        return sb.toString();
    }

}
