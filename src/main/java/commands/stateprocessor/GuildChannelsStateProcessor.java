package commands.stateprocessor;

import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import core.TextManager;
import core.atomicassets.AtomicGuildChannel;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GuildChannelsStateProcessor extends AbstractLongListProcessor<GuildChannelsStateProcessor> {

    private Permission[] checkPermissions = new Permission[0];
    private Permission[] checkPermissionsParentCategory = new Permission[0];

    public GuildChannelsStateProcessor(NavigationAbstract command, int state, int stateBack, String propertyName) {
        super(command, state, stateBack, propertyName, TextManager.getString(command.getLocale(), TextManager.COMMANDS, "stateprocessor_channels_desc"),
                EntitySelectMenu.SelectTarget.CHANNEL, EntitySelectMenu.DefaultValue::channel);
        setGetter(Collections::emptyList);
    }

    public GuildChannelsStateProcessor setCheckPermissions(Permission... checkPermissions) {
        this.checkPermissions = checkPermissions;
        return this;
    }

    public GuildChannelsStateProcessor setCheckPermissionsParentCategory(Permission... checkPermissionsParentCategory) {
        this.checkPermissionsParentCategory = checkPermissionsParentCategory;
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

        if (checkPermissionsParentCategory.length > 0) {
            for (GuildChannel channel : channels) {
                Category category = JDAUtil.getChannelParentCategory(channel);
                if (category != null && !BotPermissionUtil.can(category, checkPermissionsParentCategory)) {
                    StringBuilder sb = new StringBuilder();
                    for (Permission permission : checkPermissionsParentCategory) {
                        if (!sb.isEmpty()) {
                            sb.append(", ");
                        }
                        sb.append(TextManager.getString(getCommand().getLocale(), TextManager.PERMISSIONS, permission.name()));
                    }

                    String str = TextManager.getString(getCommand().getLocale(), TextManager.COMMANDS, "stateprocessor_channels_missingpermissions", sb.toString(), new AtomicGuildChannel(category).getPrefixedName(getCommand().getLocale()));
                    getCommand().setLog(LogStatus.FAILURE, str);
                    return true;
                }
            }
        }

        List<Long> newValues = channels.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
        set(newValues);
        return true;
    }

}
