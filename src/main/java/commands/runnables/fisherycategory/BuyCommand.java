package commands.runnables.fisherycategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryPowerUp;
import mysql.hibernate.entity.FisheryEntity;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.fisheryusers.FisheryMemberGearData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "buy",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "📥",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "shop", "upgrade", "invest", "levelup", "b" }
)
public class BuyCommand extends NavigationAbstract implements FisheryInterface {

    private FisheryMemberData fisheryMemberData;
    private FisheryGuildData fisheryGuildData;
    private GuildData guildBean;

    public BuyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        fisheryMemberData = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        fisheryGuildData = fisheryMemberData.getFisheryGuildData();

        checkRolesWithLog(event.getGuild(), fisheryGuildData.getRoles());
        if (args.length() > 0) {
            String letters = StringUtil.filterLettersFromString(args).toLowerCase().replace(" ", "");
            long numbers = StringUtil.filterLongFromString(args);
            FisheryGear fisheryGear = FisheryGear.parse(letters);

            long amount = 1;
            if (numbers != -1) {
                if (numbers >= 1 && numbers <= 100) {
                    amount = numbers;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "100"));
                    registerNavigationListener(event.getMember());
                    return true;
                }
            }

            if (fisheryGear != null) {
                for (int j = 0; j < amount; j++) {
                    if (!buy(fisheryGear, event.getMember(), false)) {
                        break;
                    }
                }

                registerNavigationListener(event.getMember());
                return true;
            }

            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
        }

        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String inputString, int state) throws Throwable {
        return null;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                deregisterListenersWithComponentMessage();
                return false;
            } else if (i >= 0 && i < FisheryGear.values().length) {
                buy(FisheryGear.values()[i], event.getMember(), true);
                return true;
            }
            return false;
        }
        return false;
    }

    private synchronized boolean buy(FisheryGear fisheryGear, Member member, boolean transferableSlots) {
        List<Role> roles = fisheryGuildData.getRoles();
        int i = fisheryGear.ordinal();

        boolean canUseTreasureChests = slotIsValid(member.getGuild(), roles, fisheryMemberData.getMemberGear(FisheryGear.TREASURE));
        boolean canUseRoles = slotIsValid(member.getGuild(), roles, fisheryMemberData.getMemberGear(FisheryGear.ROLE));

        if (transferableSlots) {
            if (i >= FisheryGear.TREASURE.ordinal() && !canUseTreasureChests) i++;
            if (i >= FisheryGear.ROLE.ordinal() && !canUseRoles) i++;
        } else {
            if (i == FisheryGear.TREASURE.ordinal() && !canUseTreasureChests) return false;
            if (i == FisheryGear.ROLE.ordinal() && !canUseRoles) return false;
        }
        if (i > FisheryGear.values().length - 1 || i < 0) return false;
        fisheryGear = FisheryGear.values()[i];

        FisheryMemberGearData slot = fisheryMemberData.getMemberGear(fisheryGear);

        long price = slot.getPrice();
        if (slot.getGear() == FisheryGear.ROLE) {
            price = calculateRolePrice(slot);
        }

        boolean usesCoupon = fisheryMemberData.getCoupons() > 0 && slot.getGear() != FisheryGear.ROLE;
        if (usesCoupon || fisheryMemberData.getCoins() >= price) {
            upgrade(slot, price, roles, member, usesCoupon);
            setLog(LogStatus.SUCCESS, getString("levelup", getString("product_" + slot.getGear().ordinal() + "_0")));
            return true;
        } else {
            if (getLog() == null || getLog().isEmpty()) {
                setLog(LogStatus.FAILURE, getString("notenough"));
            }
            return false;
        }
    }

    private void upgrade(FisheryMemberGearData slot, long price, List<Role> roles, Member member, boolean usesCoupon) {
        if (usesCoupon) {
            fisheryMemberData.decreaseCoupons();
            if (fisheryMemberData.getCoupons() <= 0) {
                fisheryMemberData.deletePowerUp(FisheryPowerUp.SHOP);
            }
        } else {
            fisheryMemberData.changeValues(0, -price);
        }
        fisheryMemberData.levelUp(slot.getGear());

        if (slot.getGear() == FisheryGear.ROLE) {
            Fishery.synchronizeRoles(member, getGuildEntity());
            TextChannel roleUpgradeChannel = getGuildEntity().getFishery().getRoleUpgradeChannel().get().orElse(null);
            if (roleUpgradeChannel != null && PermissionCheckRuntime.botHasPermission(getLocale(), getClass(), roleUpgradeChannel, Permission.MESSAGE_SEND)) {
                String announcementText = getString("newrole", StringUtil.escapeMarkdown(member.getEffectiveName()), StringUtil.escapeMarkdown(roles.get(slot.getLevel() - 1).getName()), String.valueOf(slot.getLevel()));
                roleUpgradeChannel.sendMessage(announcementText).queue();
            }
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        List<Role> roles = fisheryGuildData.getRoles();
        FisheryEntity fishery = getGuildEntity().getFishery();

        switch (state) {
            case 0:
                ArrayList<String> options = new ArrayList<>();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("beginning") + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted());
                boolean hasCoupons = fisheryMemberData.getCoupons() > 0;
                for (FisheryMemberGearData slot : getUpgradableGears()) {
                    String productDescription = "???";
                    long price = slot.getPrice();
                    if (slot.getGear() != FisheryGear.ROLE) {
                        productDescription = getString("product_des_" + slot.getGear().ordinal(), StringUtil.numToString(slot.getDeltaEffect()));
                    } else if (roles.get(slot.getLevel()) != null) {
                        price = calculateRolePrice(slot);
                        productDescription = getString("product_des_" + slot.getGear().ordinal(), StringUtil.escapeMarkdown(roles.get(slot.getLevel()).getName()));
                    }

                    String title = getString("product_" + slot.getGear().ordinal() + "_0");
                    options.add(title);
                    eb.addField(
                            getString("product_title", hasCoupons && slot.getGear() != FisheryGear.ROLE, slot.getGear().getEmoji(), title, StringUtil.numToString(slot.getLevel()), StringUtil.numToString(price)),
                            productDescription + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(),
                            false
                    );
                }

                int roleLvl = fisheryMemberData.getMemberGear(FisheryGear.ROLE).getLevel();
                boolean powerUpBonus = fisheryMemberData.getActivePowerUps().contains(FisheryPowerUp.LOUPE);
                int coupons = fisheryMemberData.getCoupons();
                String statusCurrencies = TextManager.getString(getLocale(), Category.FISHERY,
                        coupons > 0 ? "gear_desc_ext" : "gear_desc",
                        StringUtil.numToString(fisheryMemberData.getFish()),
                        StringUtil.numToString(fisheryMemberData.getCoins()),
                        StringUtil.numToString(coupons)
                );
                String status = getString(
                        "status",
                        numToStringWithPowerUpBonus(fisheryMemberData.getMemberGear(FisheryGear.MESSAGE).getEffect(), powerUpBonus),
                        StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.DAILY).getEffect()),
                        numToStringWithPowerUpBonus(fisheryMemberData.getMemberGear(FisheryGear.VOICE).getEffect(), powerUpBonus),
                        StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.TREASURE).getEffect()),
                        roles.size() > 0 && roleLvl > 0 && roleLvl <= roles.size() ? StringUtil.escapeMarkdown(roles.get(roleLvl - 1).getName()) : "-",
                        StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.SURVEY).getEffect()),
                        StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.WORK).getEffect()),
                        fishery.getCoinGiftLimit() ? StringUtil.numToString(fisheryMemberData.getCoinsGiveReceivedMax()) : "∞"
                );

                eb.addField(getString("status_title"), StringUtil.shortenStringLine(statusCurrencies + "\n\n" + status, 1024), false);
                setComponents(options.toArray(new String[0]));
                return eb;

            case 1:
                return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("{PREFIX}", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title"));

            default:
                return null;
        }
    }

    private List<FisheryMemberGearData> getUpgradableGears() {
        List<Role> roles = fisheryGuildData.getRoles();
        Guild guild = roles.stream()
                .findFirst()
                .map(Role::getGuild)
                .orElse(null);

        return fisheryMemberData.getGearMap().values().stream()
                .filter(slot -> slotIsValid(guild, roles, slot))
                .collect(Collectors.toList());
    }

    private boolean slotIsValid(Guild guild, List<Role> roles, FisheryMemberGearData slot) {
        if (slot.getGear() == FisheryGear.ROLE) {
            return slot.getLevel() < roles.size() &&
                    BotPermissionUtil.can(guild, Permission.MANAGE_ROLES) &&
                    BotPermissionUtil.canManage(roles.get(slot.getLevel()));
        }

        if (slot.getGear() == FisheryGear.TREASURE) {
            return getGuildEntity().getFishery().getTreasureChests();
        }

        return true;
    }

    private long calculateRolePrice(FisheryMemberGearData slot) {
        FisheryEntity fishery = getGuildEntity().getFishery();
        return Fishery.getFisheryRolePrice(fishery.getRolePriceMin(), fishery.getRolePriceMax(), fisheryGuildData.getRoles().size(), slot.getLevel());
    }

    private String numToStringWithPowerUpBonus(long value, boolean powerUpBonus) {
        String str = StringUtil.numToString(value);
        if (powerUpBonus) {
            str += " (+" + StringUtil.numToString(Math.round(value * 0.25)) + ")";
        }
        return str;
    }

}
