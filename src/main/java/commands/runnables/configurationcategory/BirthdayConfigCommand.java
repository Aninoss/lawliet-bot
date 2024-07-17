package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.RolesStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.BirthdayConfigEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "birthdayconfig",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "🎁",
        executableWithoutArgs = true,
        releaseDate = {2024, 8, 15},
        usesExtEmotes = true,
        aliases = {"birthdaysettings"}
)
public class BirthdayConfigCommand extends NavigationAbstract {

    public static final int STATE_SET_CHANNEL = 1,
            STATE_SET_ROLE = 2;

    public BirthdayConfigCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_CHANNEL, DEFAULT_STATE, getString("home_channel"))
                        .setMinMax(1, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                        .setLogEvent(BotLogEntity.Event.BIRTHDAY_CONFIG_CHANNEL)
                        .setSingleGetter(() -> getGuildEntity().getBirthdayConfig().getChannelId())
                        .setSingleSetter(channelId -> getGuildEntity().getBirthdayConfig().setChannelId(channelId)),
                new RolesStateProcessor(this, STATE_SET_ROLE, DEFAULT_STATE, getString("home_role"))
                        .setMinMax(1, 1)
                        .setCheckAccess(true)
                        .setLogEvent(BotLogEntity.Event.BIRTHDAY_CONFIG_ROLE)
                        .setSingleGetter(() -> getGuildEntity().getBirthdayConfig().getRoleId())
                        .setSingleSetter(roleId -> getGuildEntity().getBirthdayConfig().setRoleId(roleId))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                BirthdayConfigEntity birthdayConfig = getGuildEntity().getBirthdayConfig();
                birthdayConfig.beginTransaction();
                birthdayConfig.setActive(!birthdayConfig.getActive());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.BIRTHDAY_CONFIG_ACTIVE, event.getMember(), null, birthdayConfig.getActive());
                birthdayConfig.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("log_setactive", birthdayConfig.getActive()));
                return true;
            }
            case 1 -> {
                setState(STATE_SET_CHANNEL);
                return true;
            }
            case 2 -> {
                setState(STATE_SET_ROLE);
                return true;
            }
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        setComponents(getString("home_options").split("\n"));

        BirthdayConfigEntity birthdayConfig = getGuildEntity().getBirthdayConfig();
        return EmbedFactory.getEmbedDefault(this, getString("home_desc"))
                .addField(getString("home_active"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), birthdayConfig.getActive()), true)
                .addField(getString("home_channel"),birthdayConfig.getChannel().getPrefixedNameInField(getLocale()), true)
                .addField(getString("home_role"),birthdayConfig.getRole().getPrefixedNameInField(getLocale()), true);
    }

}
