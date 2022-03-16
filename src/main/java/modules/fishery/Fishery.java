package modules.fishery;

import java.util.HashSet;
import java.util.Locale;
import commands.Command;
import commands.runnables.fisherysettingscategory.FisheryCommand;
import commands.Category;
import core.EmbedFactory;
import core.TextManager;
import core.components.ActionRows;
import modules.JoinRoles;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

public class Fishery {

    public static void synchronizeRoles(Member member) {
        Guild guild = member.getGuild();
        FisheryGuildData fisheryGuildBean = DBFishery.getInstance().retrieve(guild.getIdLong());
        Locale locale = fisheryGuildBean.getGuildData().getLocale();
        if (fisheryGuildBean.getGuildData().getFisheryStatus() == FisheryStatus.STOPPED) {
            return;
        }

        HashSet<Role> rolesToAdd = new HashSet<>();
        HashSet<Role> rolesToRemove = new HashSet<>();
        JoinRoles.getFisheryRoles(locale, member, rolesToAdd, rolesToRemove);

        if (rolesToAdd.size() > 0 || rolesToRemove.size() > 0) {
            guild.modifyMemberRoles(member, rolesToAdd, rolesToRemove)
                    .reason(Command.getCommandLanguage(FisheryCommand.class, locale).getTitle())
                    .queue();
        }
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

    public static void spawnTreasureChest(BaseGuildMessageChannel channel) {
        GuildData guildBean = DBGuild.getInstance().retrieve(channel.getGuild().getIdLong());
        Locale locale = guildBean.getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(FisheryCommand.EMOJI_TREASURE + " " + TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_title"))
                .setDescription(TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_desription"))
                .setImage("https://cdn.discordapp.com/attachments/711665837114654781/711665915355201576/treasure_closed.png");

        Button button = Button.of(ButtonStyle.SECONDARY, "open", TextManager.getString(locale, Category.FISHERY_SETTINGS, "fishery_treasure_button"))
                .withEmoji(Emoji.fromUnicode(FisheryCommand.EMOJI_KEY));
        channel.sendMessageEmbeds(eb.build())
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
        int rateNow = ExchangeRate.get(offset);
        int rateBefore = ExchangeRate.get(offset - 1);

        if (rateNow > rateBefore) {
            return "🔺";
        } else {
            if (rateNow < rateBefore) {
                return "🔻";
            } else {
                return "•";
            }
        }
    }

}
