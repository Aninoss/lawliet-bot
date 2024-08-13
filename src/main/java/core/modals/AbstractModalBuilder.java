package core.modals;

import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.ExceptionUtil;
import core.utils.StringUtil;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.glassfish.jersey.internal.util.Producer;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractModalBuilder<T, U extends AbstractModalBuilder<T, U>> {

    private final NavigationAbstract command;
    private final String propertyName;
    private final TextInputStyle textInputStyle;
    private int minLength = 0;
    private int maxLength = TextInput.MAX_VALUE_LENGTH;
    private BotLogEntity.Event logEvent = null;
    private boolean hibernateTransaction = false;
    private Producer<T> getter = () -> null;
    private Function<T, Boolean> setter = newValue -> true;

    public AbstractModalBuilder(NavigationAbstract command, String propertyName, TextInputStyle textInputStyle) {
        this.command = command;
        this.propertyName = propertyName;
        this.textInputStyle = textInputStyle;
    }

    protected NavigationAbstract getCommand() {
        return command;
    }

    public U setMinMaxLength(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        return (U) this;
    }

    public U setLogEvent(BotLogEntity.Event logEvent) {
        this.logEvent = logEvent;
        if (logEvent != null) {
            enableHibernateTransaction();
        }
        return (U) this;
    }

    public U enableHibernateTransaction() {
        this.hibernateTransaction = true;
        return (U) this;
    }

    public U setGetter(Producer<T> getter) {
        this.getter = getter;
        return (U) this;
    }

    public U setSetter(Consumer<T> setter) {
        this.setter = t -> {
            setter.accept(t);
            return true;
        };
        return (U) this;
    }

    public U setSetterOptionalLogs(Function<T, Boolean> setter) {
        this.setter = setter;
        return (U) this;
    }

    public Modal build() {
        String ID = "value";
        T value = getter.call();
        String stringValue = value != null ? valueToString(value) : null;

        TextInput message = TextInput.create(ID, StringUtil.shortenString(getTextInputLabel(propertyName), TextInput.MAX_LABEL_LENGTH), textInputStyle)
                .setValue(stringValue != null && stringValue.isEmpty() ? null : stringValue)
                .setRequiredRange(minLength, maxLength)
                .setRequired(minLength > 0)
                .build();

        String title = StringUtil.shortenString(TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_adjust", propertyName), Modal.MAX_TITLE_LENGTH);
        Modal.Builder builder = ModalMediator.createModal(command.getMemberId().get(), title, (e, guildEntity) -> {
            e.deferEdit().queue();
            command.setGuildEntity(guildEntity);

            try {
                ModalMapping newValue = e.getValue(ID);
                process(e.getMember(), newValue != null ? newValue.getAsString() : null);
            } catch (Throwable throwable) {
                ExceptionUtil.handleCommandException(throwable, command, command.getCommandEvent(), guildEntity);
            }
        });

        return builder.addActionRow(message)
                .build();
    }

    abstract protected void process(Member member, String valueString);

    abstract protected String valueToString(T value);

    protected String getTextInputLabel(String propertyName) {
        return propertyName;
    }

    protected void set(Member member, T t) {
        EntityManagerWrapper entityManager = command.getEntityManager();
        if (hibernateTransaction) {
            entityManager.getTransaction().begin();
        }
        if (logEvent != null) {
            BotLogEntity.log(entityManager, logEvent, member.getGuild().getIdLong(), member.getIdLong(), getter.call(), t);
        }
        if (setter.apply(t)) {
            command.setLog(LogStatus.SUCCESS, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_log_success", propertyName));
        }
        if (hibernateTransaction) {
            entityManager.getTransaction().commit();
        }
        draw(member);
    }

    protected void draw(Member member) {
        command.processDraw(member, true).exceptionally(ExceptionLogger.get());
    }

}
