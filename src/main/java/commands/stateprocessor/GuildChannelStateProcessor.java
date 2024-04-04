package commands.stateprocessor;

import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.atomicassets.AtomicGuildChannel;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.Permission;
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

public class GuildChannelStateProcessor extends AbstractStateProcessor<Long, Long> {

    public static final String SELECT_MENU_ID = "entities";

    private final boolean clearButton;
    private final Collection<ChannelType> channelTypes;
    private final Permission[] checkPermissions;
    private final Producer<Long> getter;

    public GuildChannelStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, boolean clearButton,
                                      Collection<ChannelType> channelTypes, Permission[] checkPermissions,
                                      Producer<Long> getter, Consumer<Long> setter
    ) {
        this(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_channels_desc"), clearButton, channelTypes, checkPermissions, getter, setter);
    }

    public GuildChannelStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName, String description,
                                      boolean clearButton, Collection<ChannelType> channelTypes, Permission[] checkPermissions,
                                      Producer<Long> getter, Consumer<Long> setter
    ) {
        super(command, state, stateBack, propertyName, description, clearButton, setter);
        this.clearButton = clearButton;
        this.channelTypes = channelTypes;
        this.checkPermissions = checkPermissions != null ? checkPermissions : new Permission[0];
        this.getter = getter;
    }

    @Override
    public boolean controllerEntitySelectMenu(EntitySelectInteractionEvent event) {
        GuildChannel channel = event.getMentions().getChannels().get(0);

        if (checkPermissions.length > 0) {
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

        set(channel.getIdLong());
        return true;
    }

    @Override
    protected void addActionRows(ArrayList<ActionRow> actionRows) {
        Long channelId = getter.call();
        EntitySelectMenu entitySelectMenu = EntitySelectMenu.create(SELECT_MENU_ID, EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(channelTypes)
                .setDefaultValues(channelId != null ? List.of(EntitySelectMenu.DefaultValue.channel(channelId)) : Collections.emptyList())
                .setRequiredRange(clearButton ? 0 : 1, 1)
                .build();
        actionRows.add(ActionRow.of(entitySelectMenu));
    }

}
