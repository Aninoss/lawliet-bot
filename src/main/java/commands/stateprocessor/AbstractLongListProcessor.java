package commands.stateprocessor;

import commands.runnables.NavigationAbstract;
import core.utils.JDAUtil;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.glassfish.jersey.internal.util.Producer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AbstractLongListProcessor<T extends AbstractStateProcessor<List<Long>, T>> extends AbstractStateProcessor<List<Long>, T> {

    public static final String SELECT_MENU_ID = "entities";

    private final EntitySelectMenu.SelectTarget selectTarget;
    private final Function<Long, EntitySelectMenu.DefaultValue> defaultValueFunction;
    private int min = 0;
    private int max = EntitySelectMenu.OPTIONS_MAX_AMOUNT;
    private Collection<ChannelType> channelTypes = JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES;

    public AbstractLongListProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, String description,
                                     EntitySelectMenu.SelectTarget selectTarget, Function<Long, EntitySelectMenu.DefaultValue> defaultValueFunction
    ) {
        super(command, state, stateBack, propertyName, description);
        this.selectTarget = selectTarget;
        this.defaultValueFunction = defaultValueFunction;
        setGetter(Collections::emptyList);
    }

    public T setMinMax(int min, int max) {
        this.min = min;
        this.max = max;
        return (T) this;
    }

    public T setChannelTypes(Collection<ChannelType> channelTypes) {
        this.channelTypes = channelTypes;
        return (T) this;
    }

    public T setSingleGetter(Producer<Long> getter) {
        setGetter(() -> {
            Long value = getter.call();
            return value == null ? Collections.emptyList() : List.of(value);
        });
        return (T) this;
    }

    public T setSingleSetter(Consumer<Long> setter) {
        setSetter(list -> setter.accept(list.isEmpty() ? null : list.get(0)));
        return (T) this;
    }

    @Override
    protected void addActionRows(ArrayList<ActionRow> actionRows) {
        List<Long> valueList = get();
        if (valueList == null) {
            valueList = Collections.emptyList();
        }

        List<EntitySelectMenu.DefaultValue> defaultValues = valueList.stream()
                .filter(id -> id != null && id != 0L)
                .map(defaultValueFunction)
                .limit(max)
                .collect(Collectors.toList());

        EntitySelectMenu entitySelectMenu = EntitySelectMenu.create(SELECT_MENU_ID, selectTarget)
                .setChannelTypes(channelTypes)
                .setDefaultValues(defaultValues)
                .setRequiredRange(min, max)
                .build();
        actionRows.add(ActionRow.of(entitySelectMenu));
    }

}
