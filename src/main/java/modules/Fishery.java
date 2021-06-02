package modules;

import java.util.Locale;
import commands.Command;
import commands.runnables.fisherysettingscategory.FisheryCommand;
import constants.Category;
import constants.FisheryGear;
import constants.FisheryStatus;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.components.ActionRows;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

public class Fishery {

    public static void giveRoles(Member member) {
        Guild guild = member.getGuild();
        FisheryGuildData fisheryGuildBean = DBFishery.getInstance().retrieve(guild.getIdLong());
        Locale locale = fisheryGuildBean.getGuildBean().getLocale();
        if (fisheryGuildBean.getGuildBean().getFisheryStatus() == FisheryStatus.STOPPED) {
            return;
        }

        fisheryGuildBean.getMemberBean(member.getIdLong())
                .getRoles()
                .forEach(role -> {
                    if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, FisheryCommand.class, role)) {
                        guild.addRoleToMember(member, role)
                                .reason(Command.getCommandLanguage(FisheryCommand.class, locale).getTitle())
                                .queue();
                    }
                });
    }

    public static long getFisheryRolePrice(Guild guild, int size, int n) {
        GuildData guildBean = DBGuild.getInstance().retrieve(guild.getIdLong());

        double priceIdealMin = guildBean.getFisheryRoleMin();
        double priceIdealMax = guildBean.getFisheryRoleMax();

        if (size == 1) {
            return (long) priceIdealMin;
        }

        double power = Math.pow(priceIdealMax / priceIdealMin, 1 / (double) (size - 1));

        double price = Math.pow(power, n);
        double priceMax = Math.pow(power, size - 1);

        return Math.round(price * (priceIdealMax / priceMax));
    }

    public static long getClaimValue(FisheryMemberData userBean) {
        return Math.round(userBean.getMemberGear(FisheryGear.DAILY).getEffect() * 0.25);
    }

    public static void spawnTreasureChest(TextChannel channel) {
        GuildData guildBean = DBGuild.getInstance().retrieve(channel.getGuild().getIdLong());
        Locale locale = guildBean.getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_TREASURE + " " + TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_title"))
                .setDescription(TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_desription"))
                .setImage("https://cdn.discordapp.com/attachments/711665837114654781/711665915355201576/treasure_closed.png");

        Button button = Button.of(ButtonStyle.SECONDARY, "open", TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_button"))
                .withEmoji(Emoji.fromUnicode(FisheryCommand.EMOJI_KEY));
        channel.sendMessage(eb.build())
                .setActionRows(ActionRows.of(button))
                .queue(m -> {
                    DBStaticReactionMessages.getInstance().retrieve(channel.getGuild().getIdLong())
                            .put(m.getIdLong(), new StaticReactionMessageData(m, Command.getCommandProperties(FisheryCommand.class).trigger()));
                });
    }

    public static String getChangeEmoji() {
        return getChangeEmoji(0);
    }

    public static String getChangeEmoji(int offset) {
        int rateNow = ExchangeRate.getInstance().get(offset);
        int rateBefore = ExchangeRate.getInstance().get(offset - 1);

        if (rateNow > rateBefore) {
            return "\uD83D\uDD3A";
        } else {
            if (rateNow < rateBefore) {
                return "\uD83D\uDD3B";
            } else {
                return "•";
            }
        }
    }

}
