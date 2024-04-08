package core.modals;

import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class IntModalBuilder extends AbstractModalBuilder<Integer, IntModalBuilder> {

    private int min = 0;
    private int max = Integer.MAX_VALUE;

    public IntModalBuilder(NavigationAbstract command, String propertyName) {
        super(command, propertyName, TextInputStyle.SHORT);
    }

    public IntModalBuilder setMinMax(int min, int max) {
        this.min = min;
        this.max = max;
        setMinMaxLength(min > 0 ? 1 : 0, (int) Math.ceil(Math.log10(max + 1)));
        return this;
    }

    @Override
    protected void process(Member member, String valueString) {
        if (valueString != null && !StringUtil.stringIsInt(valueString)) {
            getCommand().setLog(LogStatus.FAILURE, TextManager.getString(getCommand().getLocale(), TextManager.GENERAL, "invalid", valueString));
            draw(member);
            return;
        }

        int newValueInt = valueString == null ? 0 : Integer.parseInt(valueString);
        if (newValueInt < min || newValueInt > max) {
            getCommand().setLog(LogStatus.FAILURE, TextManager.getString(getCommand().getLocale(), TextManager.GENERAL, "number", StringUtil.numToString(min), StringUtil.numToString(max)));
            draw(member);
            return;
        }

        set(member, newValueInt);
    }

    @Override
    protected String valueToString(Integer value) {
        return String.valueOf(value);
    }


}
