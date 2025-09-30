package commands.runnables.fisherysettingscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.RolesStateProcessor;
import constants.LogStatus;
import constants.Settings;
import core.EmbedFactory;
import core.ListGen;
import core.atomicassets.AtomicRole;
import core.modals.ModalMediator;
import core.utils.CollectionUtil;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.FisheryEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "fisheryroles",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        botGuildPermissions = Permission.MANAGE_ROLES,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "📜",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"fishingroles", "fishroles", "fisheryr", "fisheryrole"}
)
public class FisheryRolesCommand extends NavigationAbstract {

    public static final int MAX_ROLES = 25;

    public static final int STATE_SET_ROLES = 1,
            STATE_SET_ANNOUNCEMENT_CHANNEL = 2;

    public FisheryRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        checkRolesWithLog(event.getGuild(), getRoles());
        registerNavigationListener(event.getMember(), List.of(
                new RolesStateProcessor(this, STATE_SET_ROLES, DEFAULT_STATE, getString("state0_mroles"))
                        .setCheckAccess(true)
                        .setMinMax(0, MAX_ROLES)
                        .setDescription(getString("state1_description"))
                        .setLogEvent(BotLogEntity.Event.FISHERY_ROLES)
                        .setGetter(() -> getGuildEntity().getFishery().getRoleIds())
                        .setSetter(roleIds -> CollectionUtil.replace(getGuildEntity().getFishery().getRoleIds(), roleIds)),
                new GuildChannelsStateProcessor(this, STATE_SET_ANNOUNCEMENT_CHANNEL, DEFAULT_STATE, getString("state0_mannouncementchannel"))
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                        .setMinMax(0, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setDescription(getString("state2_description"))
                        .setLogEvent(BotLogEntity.Event.FISHERY_ROLES_UPGRADE_CHANNEL)
                        .setSingleGetter(() -> getGuildEntity().getFishery().getRoleUpgradeChannelId())
                        .setSingleSetter(channelId -> getGuildEntity().getFishery().setRoleUpgradeChannelId(channelId))
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        FisheryEntity fishery = getGuildEntity().getFishery();
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                setState(STATE_SET_ROLES);
                return true;

            case 1:
                fishery.beginTransaction();
                fishery.setSingleRoles(!fishery.getSingleRoles());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES_SINGLE_ROLES, event.getMember(), null, fishery.getSingleRoles());
                fishery.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("singleroleset", fishery.getSingleRoles()));
                return true;

            case 2:
                setState(STATE_SET_ANNOUNCEMENT_CHANNEL);
                return true;

            case 3:
                String minId = "min";
                TextInput textMin = TextInput.create(minId, TextInputStyle.SHORT)
                        .setValue(StringUtil.numToString(fishery.getRolePriceMin()))
                        .setMinLength(1)
                        .setMaxLength(21)
                        .build();

                String maxId = "max";
                TextInput textMax = TextInput.create(maxId, TextInputStyle.SHORT)
                        .setValue(StringUtil.numToString(fishery.getRolePriceMax()))
                        .setMinLength(1)
                        .setMaxLength(21)
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("state0_mroleprices"), e -> {
                            String minStr = e.getValue(minId).getAsString();
                            long priceMin = Math.min(Settings.FISHERY_MAX, MentionUtil.getAmountExt(minStr));
                            if (priceMin < 0) {
                                setLog(LogStatus.FAILURE, getString("invalid_min", StringUtil.escapeMarkdown(minStr)));
                                return null;
                            }

                            String maxStr = e.getValue(maxId).getAsString();
                            long priceMax = Math.min(Settings.FISHERY_MAX, MentionUtil.getAmountExt(maxStr));
                            if (priceMax < 0) {
                                setLog(LogStatus.FAILURE, getString("invalid_max", StringUtil.escapeMarkdown(maxStr)));
                                return null;
                            }
                            if (priceMin > priceMax) {
                                setLog(LogStatus.FAILURE, getString("invalid_outoforder"));
                                return null;
                            }

                            FisheryEntity newFishery = getGuildEntity().getFishery();
                            newFishery.beginTransaction();
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES_PRICE_MIN, e.getMember(), newFishery.getRolePriceMin(), priceMin);
                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES_PRICE_MAX, e.getMember(), newFishery.getRolePriceMax(), priceMax);
                            newFishery.setRolePriceMin(priceMin);
                            newFishery.setRolePriceMax(priceMax);
                            newFishery.commitTransaction();

                            setLog(LogStatus.SUCCESS, getString("pricesset"));
                            return null;
                        })
                        .addComponents(
                                Label.of(getString("firstprice"), textMin),
                                Label.of(getString("lastprice"), textMax)
                        )
                        .build();

                event.replyModal(modal).queue();
                return false;

            default:
                return false;
        }
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        FisheryEntity fishery = getGuildEntity().getFishery();

        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description", String.valueOf(MAX_ROLES)))
                .addField(getString("state0_mroles"), new ListGen<Role>().getList(getRoles(), getLocale(), this::getRoleString), false)
                .addField(getString("state0_msinglerole", StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), fishery.getSingleRoles())), getString("state0_msinglerole_desc"), false)
                .addField(getString("state0_mannouncementchannel"), fishery.getRoleUpgradeChannel().getPrefixedNameInField(getLocale()), true)
                .addField(getString("state0_mroleprices"), getString("state0_mroleprices_desc", StringUtil.numToString(fishery.getRolePriceMin()), StringUtil.numToString(fishery.getRolePriceMax())), true);
    }

    private String getRoleString(Role role) {
        List<Role> roles = getRoles();
        int n = roles.indexOf(role);
        FisheryEntity fishery = getGuildEntity().getFishery();
        return getString(
                "state0_rolestring",
                new AtomicRole(role).getPrefixedNameInField(getLocale()),
                StringUtil.numToString(Fishery.getFisheryRolePrice(fishery.getRolePriceMin(), fishery.getRolePriceMax(), roles.size(), n))
        );
    }

    private List<Role> getRoles() {
        return getGuildEntity().getFishery().getRoles();
    }

}
