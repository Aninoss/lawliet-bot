package commands.runnables.fisherycategory;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryMemberAccountInterface;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryPowerUp;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.fisheryusers.FisheryMemberGearData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

@CommandProperties(
        trigger = "gear",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83C\uDFA3",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        requiresFullMemberCache = true,
        aliases = { "equip", "equipment", "inventory", "level", "g" }
)
public class GearCommand extends FisheryMemberAccountInterface {

    public GearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        List<Role> buyableRoles = DBFishery.getInstance().retrieve(member.getGuild().getIdLong()).getRoles();
        FisheryMemberData fisheryMemberData = DBFishery.getInstance().retrieve(member.getGuild().getIdLong()).getMemberData(member.getIdLong());
        int coupons = fisheryMemberData.getCoupons();
        String desc = getString(
                coupons > 0 ? "desc_ext" : "desc",
                StringUtil.numToString(fisheryMemberData.getFish()),
                StringUtil.numToString(fisheryMemberData.getCoins()),
                StringUtil.numToString(coupons)
        );
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(desc);
        EmbedUtil.setFooter(eb, this);

        boolean patreon = PatreonCache.getInstance().hasPremium(member.getIdLong(), false);
        String patreonEmoji = "\uD83D\uDC51";
        String displayName = member.getEffectiveName();
        while (displayName.length() > 0 && displayName.startsWith(patreonEmoji)) {
            displayName = displayName.substring(patreonEmoji.length());
        }

        eb.setAuthor(TextManager.getString(getLocale(), TextManager.GENERAL, "rankingprogress_title", patreon, displayName, patreonEmoji), null, member.getEffectiveAvatarUrl())
                .setThumbnail(member.getEffectiveAvatarUrl());
        if (patreon) eb.setColor(Color.YELLOW);

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
                buyableRoles.size() > 0 && roleLvl > 0 && roleLvl <= buyableRoles.size() ? StringUtil.escapeMarkdown(buyableRoles.get(roleLvl - 1).getName()) : "-",
                StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.SURVEY).getEffect()),
                StringUtil.numToString(fisheryMemberData.getMemberGear(FisheryGear.WORK).getEffect()),
                fisheryMemberData.getGuildData().hasFisheryCoinsGivenLimit() ? StringUtil.numToString(fisheryMemberData.getCoinsGiveReceivedMax()) : "∞"
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

}