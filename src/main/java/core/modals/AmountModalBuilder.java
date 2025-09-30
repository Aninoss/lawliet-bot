package core.modals;

import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;

public class AmountModalBuilder extends AbstractModalBuilder<Long, AmountModalBuilder> {

    private long min = 0;
    private long max = Long.MAX_VALUE - 1;

    public AmountModalBuilder(NavigationAbstract command, String propertyName) {
        super(command, propertyName, TextInputStyle.SHORT);
    }

    public AmountModalBuilder setMinMax(long min, long max) {
        this.min = min;
        this.max = max;
        setMinMaxLength(min > 0 ? 1 : 0, 50);
        return this;
    }

    @Override
    protected void process(Member member, String valueString) {
        long newValueLong = MentionUtil.getAmountExt(valueString, max);
        if (newValueLong < min || newValueLong > max) {
            getCommand().setLog(LogStatus.FAILURE, TextManager.getString(getCommand().getLocale(), TextManager.GENERAL, "number", StringUtil.numToString(min), StringUtil.numToString(max)));
            draw(member);
            return;
        }

        set(member, newValueLong);
    }

    @Override
    protected String valueToString(Long value) {
        return String.valueOf(value);
    }


}
