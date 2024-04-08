package core.modals;

import commands.runnables.NavigationAbstract;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

public class StringModalBuilder extends AbstractModalBuilder<String, StringModalBuilder> {

    public StringModalBuilder(NavigationAbstract command, String propertyName, TextInputStyle textInputStyle) {
        super(command, propertyName, textInputStyle);
    }

    @Override
    protected void process(Member member, String valueString) {
        set(member, valueString != null && valueString.isEmpty() ? null : valueString);
    }

    @Override
    protected String valueToString(String value) {
        return value;
    }


}
