package commands.runnables.fisherysettingscategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Settings;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.FisheryEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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
        aliases = { "fishingroles", "fishroles", "fisheryr", "fisheryrole" }
)
public class FisheryRolesCommand extends NavigationAbstract {

    public static final int MAX_ROLES = 50;

    public FisheryRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        checkRolesWithLog(event.getGuild(), getRoles());
        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        FisheryEntity fishery = getGuildEntity().getFishery();

        switch (state) {
            case 1:
                List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
                if (roleList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else {
                    if (!checkRolesWithLog(event.getMember(), roleList)) {
                        return MessageInputResponse.FAILED;
                    }

                    List<Role> roles = getRoles();
                    int existingRoles = 0;
                    for (Role role : roleList) {
                        if (roles.contains(role)) {
                            existingRoles++;
                        }
                    }

                    if (existingRoles >= roleList.size()) {
                        setLog(LogStatus.FAILURE, getString("roleexists", roleList.size() != 1));
                        return MessageInputResponse.FAILED;
                    }

                    ArrayList<String> roleIds = new ArrayList<>();
                    fishery.beginTransaction();
                    for (Role role : roleList) {
                        if (!roles.contains(role)) {
                            fishery.getRoleIds().add(role.getIdLong());
                            roleIds.add(role.getId());
                        }
                    }
                    fishery.commitTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES, event.getMember(), roleIds, null);

                    int rolesAdded = roleIds.size();
                    setLog(LogStatus.SUCCESS, getString("roleadd", (rolesAdded - existingRoles) != 1, String.valueOf(rolesAdded)));
                    setState(0);
                    return MessageInputResponse.SUCCESS;
                }

            case 3:
                List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
                if (channelList.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return MessageInputResponse.FAILED;
                } else {
                    TextChannel channel = channelList.get(0);
                    if (checkWriteEmbedInChannelWithLog(channel)) {
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES_UPGRADE_CHANNEL, event.getMember(), fishery.getRoleUpgradeChannelId(), channel.getIdLong());
                        fishery.beginTransaction();
                        fishery.setRoleUpgradeChannelId(channel.getIdLong());
                        fishery.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                        setState(0);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        return MessageInputResponse.FAILED;
                    }
                }

            case 4:
                if (input.contains("-") && !input.replaceFirst("-", "").contains("-")) {
                    String[] parts = (input + " ").split("-");
                    long priceMin = MentionUtil.getAmountExt(parts[0]);
                    long priceMax = MentionUtil.getAmountExt(parts[1]);

                    if (priceMin >= -1 && priceMax >= -1 && priceMin <= Settings.FISHERY_MAX && priceMax <= Settings.FISHERY_MAX) {
                        if (priceMin == -1) priceMin = fishery.getRolePriceMin();
                        if (priceMax == -1) priceMax = fishery.getRolePriceMax();

                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES_PRICE_MIN, event.getMember(), fishery.getRolePriceMin(), priceMin);
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES_PRICE_MAX, event.getMember(), fishery.getRolePriceMax(), priceMax);
                        fishery.beginTransaction();
                        fishery.setRolePriceMin(priceMin);
                        fishery.setRolePriceMax(priceMax);
                        fishery.commitTransaction();

                        setLog(LogStatus.SUCCESS, getString("pricesset"));
                        setState(0);
                        return MessageInputResponse.SUCCESS;
                    } else {
                        setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "0", StringUtil.numToString(Settings.FISHERY_MAX)));
                        return MessageInputResponse.FAILED;
                    }
                } else {
                    setLog(LogStatus.FAILURE, getString("prices_wrongvalues"));
                    return MessageInputResponse.FAILED;
                }

            default:
                return null;
        }
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        FisheryEntity fishery = getGuildEntity().getFishery();

        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deregisterListenersWithComponentMessage();
                        return false;

                    case 0:
                        if (getRoles().size() < MAX_ROLES) {
                            setState(1);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanyroles", String.valueOf(MAX_ROLES)));
                            return true;
                        }

                    case 1:
                        if (!getRoles().isEmpty()) {
                            setState(2);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("norolesset"));
                            return true;
                        }

                    case 2:
                        fishery.beginTransaction();
                        fishery.setSingleRoles(!fishery.getSingleRoles());
                        fishery.commitTransaction();
                        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES_SINGLE_ROLES, event.getMember(), null, fishery.getSingleRoles());

                        setLog(LogStatus.SUCCESS, getString("singleroleset", fishery.getSingleRoles()));
                        return true;

                    case 3:
                        setState(3);
                        return true;

                    case 4:
                        setState(4);
                        return true;

                    default:
                        return false;
                }

            case 1:

            case 4:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                return false;

            case 2:
                List<Role> roles = getRoles();
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < roles.size()) {
                    fishery.beginTransaction();
                    fishery.getRoleIds().remove(roles.get(i).getIdLong());
                    fishery.commitTransaction();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES, event.getMember(), null, roles.get(i).getId());

                    setLog(LogStatus.SUCCESS, getString("roleremove"));
                    if (getRoles().isEmpty()) {
                        setState(0);
                    }
                    return true;
                }
                return false;

            case 3:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i == 0) {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_ROLES_UPGRADE_CHANNEL, event.getMember(), fishery.getRoleUpgradeChannelId(), null);
                    fishery.beginTransaction();
                    fishery.setRoleUpgradeChannelId(null);
                    fishery.commitTransaction();

                    setState(0);
                    setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                    return true;
                }
                return false;

            default:
                return false;
        }
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

    private String getRoleString2(Role role) {
        List<Role> roles = getRoles();
        int n = roles.indexOf(role);
        FisheryEntity fishery = getGuildEntity().getFishery();
        return getString(
                "state2_rolestring",
                role.getName(), StringUtil.numToString(Fishery.getFisheryRolePrice(fishery.getRolePriceMin(), fishery.getRolePriceMax(), roles.size(), n))
        );
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        FisheryEntity fishery = getGuildEntity().getFishery();
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setComponents(getString("state0_options").split("\n"));

                return EmbedFactory.getEmbedDefault(this, getString("state0_description", String.valueOf(MAX_ROLES)))
                        .addField(getString("state0_mroles"), new ListGen<Role>().getList(getRoles(), getLocale(), this::getRoleString), false)
                        .addField(getString("state0_msinglerole", StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), fishery.getSingleRoles())), getString("state0_msinglerole_desc"), false)
                        .addField(getString("state0_mannouncementchannel"), fishery.getRoleUpgradeChannel().getPrefixedNameInFieldOrElse(notSet), true)
                        .addField(getString("state0_mroleprices"), getString("state0_mroleprices_desc", StringUtil.numToString(fishery.getRolePriceMin()), StringUtil.numToString(fishery.getRolePriceMax())), true);

            case 1:
                return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));

            case 2:
                List<Role> roles = getRoles();
                String[] roleStrings = new String[roles.size()];
                for (int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = getRoleString2(roles.get(i));
                }
                setComponents(roleStrings);

                return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));

            case 3:
                setComponents(getString("state3_options"));
                return EmbedFactory.getEmbedDefault(this, getString("state3_description"), getString("state3_title"));

            case 4:
                return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));

            default:
                return null;
        }
    }

    private List<Role> getRoles() {
        return getGuildEntity().getFishery().getRoles();
    }

}
