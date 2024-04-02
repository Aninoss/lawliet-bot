package core.modals;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import commands.listeners.Drawable;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

public class ModalMediator {

    private static final Cache<String, ModalConsumer> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .build();

    public static Modal.Builder createModal(String title, ModalConsumer consumer) {
        String customId = RandomUtil.generateRandomString(20);
        cache.put(customId, consumer);
        return Modal.create(customId, title);
    }

    public static Modal.Builder createDrawableCommandModal(Command command, String title, Function<ModalInteractionEvent, EmbedBuilder> consumer) {
        return createModal(title, (e, guildEntity) -> {
            e.deferEdit().queue();
            command.setGuildEntity(guildEntity);

            try {
                EmbedBuilder eb = consumer.apply(e);

                if (command instanceof NavigationAbstract) {
                    NavigationAbstract navigationAbstractCommand = (NavigationAbstract) command;
                    navigationAbstractCommand.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
                    return;
                }

                if (eb == null && command instanceof Drawable) {
                    Drawable drawableCommand = (Drawable) command;
                    eb = drawableCommand.draw(e.getMember());
                }
                command.drawMessage(eb).exceptionally(ExceptionLogger.get());
            } catch (Throwable throwable) {
                ExceptionUtil.handleCommandException(throwable, command, command.getCommandEvent(), guildEntity);
            }
        });
    }

    public static Modal createStringModal(NavigationAbstract command, String valueName, TextInputStyle textInputStyle,
                                          int minLength, int maxLength, String value, Consumer<String> setter
    ) {
        return createStringModalWithOptionalLog(command, valueName, textInputStyle, minLength, maxLength, value, newValue -> {
            setter.accept(newValue);
            return true;
        });
    }

    public static Modal createStringModalWithOptionalLog(NavigationAbstract command, String valueName, TextInputStyle textInputStyle,
                                                         int minLength, int maxLength, String value, Function<String, Boolean> setter
    ) {
        String ID = "value";
        TextInput message = TextInput.create(ID, valueName, textInputStyle)
                .setValue(value == null || value.isEmpty() ? null : value)
                .setRequiredRange(minLength, maxLength)
                .setRequired(minLength > 0)
                .build();

        Modal.Builder builder = createModal(TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_adjust", valueName), (e, guildEntity) -> {
            e.deferEdit().queue();
            command.setGuildEntity(guildEntity);

            try {
                ModalMapping newValue = e.getValue(ID);
                String newValueString = newValue != null ? newValue.getAsString() : null;

                if (setter.apply(newValueString != null && newValueString.isEmpty() ? null : newValueString)) {
                    command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_log_success", valueName));
                }
                command.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
            } catch (Throwable throwable) {
                ExceptionUtil.handleCommandException(throwable, command, command.getCommandEvent(), guildEntity);
            }
        });

        return builder.addActionRow(message)
                .build();
    }

    public static Modal createIntModal(NavigationAbstract command, String valueName, int min, int max, int value, Consumer<Integer> setter) {
        String ID = "value";
        TextInput message = TextInput.create(ID, valueName, TextInputStyle.SHORT)
                .setValue(String.valueOf(value))
                .setRequiredRange(min > 0 ? 1 : 0, (int) Math.ceil(Math.log10(max + 1)))
                .setRequired(min > 0)
                .build();

        Modal.Builder builder = createModal(TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_adjust", valueName), (e, guildEntity) -> {
            e.deferEdit().queue();
            command.setGuildEntity(guildEntity);

            try {
                ModalMapping newValue = e.getValue(ID);
                String newValueString = newValue != null ? newValue.getAsString() : null;
                if (newValueString != null && !StringUtil.stringIsInt(newValueString)) {
                    command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "invalid", newValueString));
                    command.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
                    return;
                }

                int newValueInt = newValueString == null ? 0 : Integer.parseInt(newValueString);
                if (newValueInt < min || newValueInt > max) {
                    command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "number", StringUtil.numToString(min), StringUtil.numToString(max)));
                    command.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
                    return;
                }

                setter.accept(newValueInt);
                command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_log_success", valueName));
                command.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
            } catch (Throwable throwable) {
                ExceptionUtil.handleCommandException(throwable, command, command.getCommandEvent(), guildEntity);
            }
        });

        return builder.addActionRow(message)
                .build();
    }

    public static Modal createDurationModal(NavigationAbstract command, String valueName, long minMinutes, long maxMinutes, long value, Consumer<Long> setter) {
        String ID = "value";
        TextInput message = TextInput.create(ID, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_duration", valueName), TextInputStyle.SHORT)
                .setValue(TimeUtil.getDurationString(Duration.ofMinutes(value)))
                .setRequiredRange(minMinutes > 0 ? 1 : 0, 12)
                .setRequired(minMinutes > 0)
                .build();

        Modal.Builder builder = createModal(TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_adjust", valueName), (e, guildEntity) -> {
            e.deferEdit().queue();
            command.setGuildEntity(guildEntity);

            try {
                ModalMapping newValue = e.getValue(ID);
                String newValueString = newValue != null ? newValue.getAsString() : null;
                long newValueLong = newValueString != null ? MentionUtil.getTimeMinutes(newValueString).getValue() : -1;

                if (newValueLong == 0L) {
                    command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.GENERAL, "invalid", newValueString));
                    command.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
                    return;
                }

                newValueLong = newValueLong != -1 ? newValueLong : 0L;
                if (newValueLong < minMinutes || newValueLong > maxMinutes) {
                    command.setLog(LogStatus.FAILURE, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_duration_outofrange",
                            TimeUtil.getDurationString(Duration.ofMinutes(minMinutes)),
                            TimeUtil.getDurationString(Duration.ofMinutes(maxMinutes))
                    ));
                    command.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
                    return;
                }

                setter.accept(newValueLong);
                command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_log_success", valueName));
                command.processDraw(e.getMember(), true).exceptionally(ExceptionLogger.get());
            } catch (Throwable throwable) {
                ExceptionUtil.handleCommandException(throwable, command, command.getCommandEvent(), guildEntity);
            }
        });

        return builder.addActionRow(message)
                .build();
    }

    public static ModalConsumer get(String customId) {
        ModalConsumer consumer = cache.getIfPresent(customId);
        cache.invalidate(customId);
        return consumer;
    }

}
