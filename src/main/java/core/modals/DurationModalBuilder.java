package core.modals;

import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.TimeUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.glassfish.jersey.internal.util.Producer;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

public class DurationModalBuilder extends AbstractModalBuilder<Long, DurationModalBuilder> {

    private long minMinutes = 0;
    private long maxMinutes = Long.MAX_VALUE;

    public DurationModalBuilder(NavigationAbstract command, String propertyName) {
        super(command, propertyName, TextInputStyle.SHORT);
    }

    public DurationModalBuilder setMinMaxMinutes(long minMinutes, long maxMinutes) {
        this.minMinutes = minMinutes;
        this.maxMinutes = maxMinutes;
        setMinMaxLength(minMinutes > 0 ? 1 : 0, 12);
        return this;
    }

    @Override
    protected void process(Member member, String valueString) {
        if (minMinutes == 0 && (valueString == null || valueString.isEmpty())) {
            set(member, null);
            return;
        }

        long newValueLong = valueString != null ? MentionUtil.getTimeMinutes(valueString).getValue() : -1;
        if (newValueLong == 0L) {
            getCommand().setLog(LogStatus.FAILURE, TextManager.getString(getCommand().getLocale(), TextManager.GENERAL, "invalid", valueString));
            draw(member);
            return;
        }

        newValueLong = newValueLong != -1 ? newValueLong : 0L;
        if (newValueLong < minMinutes || newValueLong > maxMinutes) {
            getCommand().setLog(LogStatus.FAILURE, TextManager.getString(getCommand().getLocale(), TextManager.COMMANDS, "stateprocessor_duration_outofrange",
                    TimeUtil.getDurationString(Duration.ofMinutes(minMinutes)),
                    TimeUtil.getDurationString(Duration.ofMinutes(maxMinutes))
            ));
            draw(member);
            return;
        }

        set(member, newValueLong);
    }

    public DurationModalBuilder setGetterInt(Producer<Integer> getter) {
        return super.setGetter(() -> {
            Integer value = getter.call();
            return value != null ? value.longValue() : null;
        });
    }

    public DurationModalBuilder setSetterInt(Consumer<Integer> setter) {
        return super.setSetter(value -> setter.accept(value != null ? value.intValue() : null));
    }

    public DurationModalBuilder setSetterIntOptionalLogs(Function<Integer, Boolean> setter) {
        return super.setSetterOptionalLogs(value -> setter.apply(value != null ? value.intValue() : null));
    }

    @Override
    protected String valueToString(Long value) {
        return TimeUtil.getDurationString(Duration.ofMinutes(value));
    }

    @Override
    protected String getTextInputLabel(String propertyName) {
        return TextManager.getString(getCommand().getLocale(), TextManager.COMMANDS, "stateprocessor_duration", propertyName);
    }

}
