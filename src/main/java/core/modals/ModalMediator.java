package core.modals;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import commands.listeners.Drawable;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.ExceptionUtil;
import core.utils.RandomUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

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

    public static Modal createSimpleStringModal(NavigationAbstract command, String valueName, TextInputStyle textInputStyle,
                                                int minLength, int maxLength, String value, Consumer<String> setter
    ) {
        String ID = "value";
        TextInput message = TextInput.create(ID, valueName, textInputStyle)
                .setValue(value)
                .setRequiredRange(minLength, maxLength)
                .setRequired(minLength > 0)
                .build();

        Modal.Builder builder = createModal(TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_adjust", valueName), (e, guildEntity) -> {
            e.deferEdit().queue();
            command.setGuildEntity(guildEntity);

            try {
                String textValue = e.getValue(ID).getAsString();
                setter.accept(textValue.isEmpty() ? null : textValue);
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
