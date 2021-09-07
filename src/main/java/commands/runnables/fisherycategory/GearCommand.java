package commands.runnables.fisherycategory;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryMemberAccountInterface;
import commands.Category;
import modules.fishery.FisheryGear;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.fisheryusers.FisheryMemberGearData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        if (!member.getUser().isBot()) {
            List<Role> buyableRoles = DBFishery.getInstance().retrieve(member.getGuild().getIdLong()).getRoles();
            FisheryMemberData fisheryMemberBean = DBFishery.getInstance().retrieve(member.getGuild().getIdLong()).getMemberData(member.getIdLong());
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setDescription(getString("desc", StringUtil.numToString(fisheryMemberBean.getFish()), StringUtil.numToString(fisheryMemberBean.getCoins())));
            EmbedUtil.setFooter(eb, this);

            boolean patreon = PatreonCache.getInstance().getUserTier(member.getIdLong(), false) >= 1;
            String patreonEmoji = "\uD83D\uDC51";
            String displayName = member.getEffectiveName();
            while (displayName.length() > 0 && displayName.startsWith(patreonEmoji)) {
                displayName = displayName.substring(patreonEmoji.length());
            }

            eb.setAuthor(TextManager.getString(getLocale(), TextManager.GENERAL, "rankingprogress_title", patreon, displayName, patreonEmoji), null, member.getUser().getEffectiveAvatarUrl())
                    .setThumbnail(member.getUser().getEffectiveAvatarUrl());
            if (patreon) eb.setColor(Color.YELLOW);

            //Gear
            StringBuilder gearString = new StringBuilder();
            for (FisheryMemberGearData slot : fisheryMemberBean.getGearMap().values()) {
                gearString.append(getString(
                        "gear_slot",
                        slot.getGear().getEmoji(),
                        TextManager.getString(getLocale(), Category.FISHERY, "buy_product_" + slot.getGear().ordinal() + "_0"),
                        String.valueOf(slot.getLevel())
                )).append("\n");
            }
            eb.addField(getString("gear_title"), gearString.toString(), false);

            int roleLvl = fisheryMemberBean.getMemberGear(FisheryGear.ROLE).getLevel();
            eb.addField(getString("stats_title"), getString(
                    "stats_content",
                    StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.MESSAGE).getEffect()),
                    StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.DAILY).getEffect()),
                    StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.VOICE).getEffect()),
                    StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.TREASURE).getEffect()),
                    buyableRoles.size() > 0 && roleLvl > 0 && roleLvl <= buyableRoles.size() ? buyableRoles.get(roleLvl - 1).getAsMention() : "**-**",
                    StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.SURVEY).getEffect()),
                    StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.WORK).getEffect()),
                    fisheryMemberBean.getGuildData().hasFisheryCoinsGivenLimit() ? StringUtil.numToString(fisheryMemberBean.getCoinsGiveReceivedMax()) : "∞"
            ), false);
            return eb;
        } else {
            return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_bots"));
        }
    }

}