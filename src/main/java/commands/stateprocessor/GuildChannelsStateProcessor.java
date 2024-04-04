package commands.stateprocessor;

import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.atomicassets.AtomicGuildChannel;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import org.glassfish.jersey.internal.util.Producer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GuildChannelsStateProcessor extends AbstractStateProcessor<List<Long>, AbstractStateProcessor.ListUpdate<Long>, GuildChannelsStateProcessor> {

    public static final String SELECT_MENU_ID = "entities";

    private int min = 0;
    private int max = EntitySelectMenu.OPTIONS_MAX_AMOUNT;
    private Collection<ChannelType> channelTypes = JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES;
    private Permission[] checkPermissions = new Permission[0];
    private Producer<List<Long>> getter = Collections::emptyList;

    public GuildChannelsStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_channels_desc"));
    }

    public GuildChannelsStateProcessor setMinMax(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public GuildChannelsStateProcessor setChannelTypes(Collection<ChannelType> channelTypes) {
        this.channelTypes = channelTypes;
        return this;
    }

    public GuildChannelsStateProcessor setCheckPermissions(Permission[] checkPermissions) {
        this.checkPermissions = checkPermissions;
        return this;
    }

    public GuildChannelsStateProcessor setGetter(Producer<List<Long>> getter) {
        this.getter = getter;
        return this;
    }

    public GuildChannelsStateProcessor setSingleGetter(Producer<Long> getter) {
        this.getter = () -> {
            Long value = getter.call();
            return value == null ? Collections.emptyList() : List.of(value);
        };
        return this;
    }

    public GuildChannelsStateProcessor setSingleSetter(Consumer<Long> setter) {
        setSetter(update -> {
            List<Long> newValues = update.getNewValues();
            setter.accept(newValues.isEmpty() ? null : newValues.get(0));
        });
        return this;
    }

    @Override
    public boolean controllerEntitySelectMenu(EntitySelectInteractionEvent event) {
        List<GuildChannel> channels = event.getMentions().getChannels();

        if (checkPermissions.length > 0) {
            for (GuildChannel channel : channels) {
                if (!BotPermissionUtil.can(channel, checkPermissions)) {
                    StringBuilder sb = new StringBuilder();
                    for (Permission permission : checkPermissions) {
                        if (!sb.isEmpty()) {
                            sb.append(", ");
                        }
                        sb.append(TextManager.getString(getCommand().getLocale(), TextManager.PERMISSIONS, permission.name()));
                    }

                    String str = TextManager.getString(getCommand().getLocale(), TextManager.COMMANDS, "stateprocessor_channels_missingpermissions", sb.toString(), new AtomicGuildChannel(channel).getPrefixedName(getCommand().getLocale()));
                    getCommand().setLog(LogStatus.FAILURE, str);
                    return true;
                }
            }
        }

        List<Long> newValues = channels.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
        set(ListUpdate.fromUpdate(getter.call(), newValues));
        return true;
    }

    @Override
    protected void addActionRows(ArrayList<ActionRow> actionRows) {
        List<Long> valueList = getter.call();
        if (valueList == null) {
            valueList = Collections.emptyList();
        }

        List<EntitySelectMenu.DefaultValue> defaultValues = valueList.stream().filter(id -> id != null && id != 0L).map(EntitySelectMenu.DefaultValue::channel).collect(Collectors.toList());
        EntitySelectMenu entitySelectMenu = EntitySelectMenu.create(SELECT_MENU_ID, EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(channelTypes)
                .setDefaultValues(defaultValues.stream().limit(max).collect(Collectors.toList()))
                .setRequiredRange(min, max)
                .build();
        actionRows.add(ActionRow.of(entitySelectMenu));
    }

}
