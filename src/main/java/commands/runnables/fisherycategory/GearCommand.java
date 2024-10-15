package commands.runnables.fisherycategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryMemberAccountInterface;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryPowerUp;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryMemberGearData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "gear",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDFA3",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        requiresFullMemberCache = true,
        aliases = {"equip", "equipment", "inventory", "level", "g"}
)
public class GearCommand extends FisheryMemberAccountInterface {

    public GearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault();
        EmbedUtil.setFooter(eb, this);

        boolean patreon = PatreonCache.getInstance().hasPremium(member.getIdLong(), false);
        String patreonEmoji = "👑";
        String displayName = member.getEffectiveName();
        while (!displayName.isEmpty() && displayName.startsWith(patreonEmoji)) {
            displayName = displayName.substring(patreonEmoji.length());
        }
        eb.setAuthor(TextManager.getString(getLocale(), TextManager.GENERAL, "rankingprogress_title", patreon, displayName, patreonEmoji), null, member.getEffectiveAvatarUrl());
        if (patreon) {
            eb.setColor(Color.YELLOW);
        }

        if (getGuildEntity().getFishery().getGraphicallyGeneratedAccountCardsEffectively()) {
            return getEmbedCard(eb, member);
        } else {
            return getEmbedDefault(eb, member);
        }
    }

    private EmbedBuilder getEmbedDefault(EmbedBuilder eb, Member member) {
        List<Role> buyableRoles = getGuildEntity().getFishery().getRoles();
        FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(member.getGuild().getIdLong()).getMemberData(member.getIdLong());

        int coupons = fisheryMemberData.getCoupons();
        String desc = getString(
                coupons > 0 ? "desc_ext" : "desc",
                StringUtil.numToString(fisheryMemberData.getFish()),
                StringUtil.numToString(fisheryMemberData.getCoins()),
                StringUtil.numToString(coupons)
        );
        eb.setDescription(desc)
                .setThumbnail(member.getEffectiveAvatarUrl());

        StringBuilder gearString = new StringBuilder();
        for (FisheryMemberGearData slot : fisheryMemberData.getGearMap().values()) {
            gearString.append(getString(
                    "gear_slot",
                    slot.getGear().getEmoji(),
                    TextManager.getString(getLocale(), Category.FISHERY, "buy_product_" + slot.getGear().ordinal() + "_0"),
                    String.valueOf(slot.getLevel())
            )).append("\n");
        }
        eb.addField(getString("gear_title"), gearString.toString(), false);

        int roleLvl = fisheryMemberData.getMemberGear(FisheryGear.ROLE).getLevel();
        boolean powerUpBonus = fisheryMemberData.getActivePowerUps().contains(FisheryPowerUp.LOUPE);
        eb.addField(getString("stats_title"), getString(
                "stats_content",
                numToStringWithPowerUpBonus(fisheryMemberData.getMemberGear(FisheryGear.MESSAGE).getEffect(), powerUpBonus),
                StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.DAILY).getEffect()),
                numToStringWithPowerUpBonus(fisheryMemberData.getMemberGear(FisheryGear.VOICE).getEffect(), powerUpBonus),
                StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.TREASURE).getEffect()),
                !buyableRoles.isEmpty() && roleLvl > 0 && roleLvl <= buyableRoles.size() ? StringUtil.escapeMarkdown(buyableRoles.get(roleLvl - 1).getName()) : "-",
                StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.SURVEY).getEffect()),
                StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.WORK).getEffect()),
                getGuildEntity().getFishery().getCoinGiftLimit() ? StringUtil.numToString(fisheryMemberData.getCoinsGiveReceivedMax()) : "∞"
        ), false);
        return eb;
    }

    private String numToStringWithPowerUpBonus(long value, boolean powerUpBonus) {
        String str = StringUtil.numToString(value);
        if (powerUpBonus) {
            str += " (+" + StringUtil.numToString(Math.round(value * 0.25)) + ")";
        }
        return str;
    }

    private EmbedBuilder getEmbedCard(EmbedBuilder eb, Member member) throws IOException {
        List<Role> roles = getGuildEntity().getFishery().getRoles();
        FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(member.getGuild().getIdLong()).getMemberData(member.getIdLong());
        int roleLvl = fisheryMemberData.getMemberGear(FisheryGear.ROLE).getLevel();
        boolean powerUpBonus = fisheryMemberData.getActivePowerUps().contains(FisheryPowerUp.LOUPE);

        eb.setImage(Fishery.generateGearCardUrl(getLocale(), fisheryMemberData, powerUpBonus, roles, roleLvl, getGuildEntity().getFishery()));
        return eb;
    }

}