package commands.runnables.fisherycategory;

import commands.listeners.CommandProperties;
import commands.runnables.FisheryMemberAccountInterface;
import constants.Category;
import constants.FisheryCategoryInterface;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import mysql.modules.fisheryusers.FisheryMemberPowerUpBean;






import java.awt.*;
import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "gear",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83C\uDFA3",
        executableWithoutArgs = true,
        aliases = { "equip", "equipment", "inventory", "level", "g" }
)
public class GearCommand extends FisheryMemberAccountInterface {

    private List<Role> buyableRoles;

    public GearCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected void init(MessageCreateEvent event, String followedString) throws Throwable {
        buyableRoles = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getRoles();
    }

    @Override
    protected EmbedBuilder generateUserEmbed(Server server, User user, boolean userIsAuthor, String followedString) throws Throwable {
        FisheryMemberBean fisheryMemberBean = DBFishery.getInstance().retrieve(server.getId()).getMemberBean(user.getId());
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(getString("desc", StringUtil.numToString(fisheryMemberBean.getFish()), StringUtil.numToString(fisheryMemberBean.getCoins())));
        EmbedUtil.setFooter(eb, this);

        boolean patron = PatreonCache.getInstance().getUserTier(user.getId()) >= 1;
        String patreonEmoji = "\uD83D\uDC51";
        String displayName = user.getDisplayName(server);
        while (displayName.length() > 0 && displayName.startsWith(patreonEmoji))
            displayName = displayName.substring(patreonEmoji.length());

        eb.setAuthor(TextManager.getString(getLocale(), TextManager.GENERAL, "rankingprogress_title", patron, displayName, patreonEmoji), "", user.getAvatar())
                .setThumbnail(user.getAvatar());
        if (patron) eb.setColor(Color.YELLOW);

        //Gear
        StringBuilder gearString = new StringBuilder();
        for (FisheryMemberPowerUpBean slot : fisheryMemberBean.getPowerUpMap().values()) {
            gearString.append(getString(
                    "gear_slot",
                    FisheryCategoryInterface.PRODUCT_EMOJIS[slot.getPowerUpId()],
                    TextManager.getString(getLocale(), Category.FISHERY, "buy_product_" + slot.getPowerUpId() + "_0"),
                    String.valueOf(slot.getLevel())
            )).append("\n");
        }
        eb.addField(getString("gear_title"), gearString.toString(), false);

        int roleLvl = fisheryMemberBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel();
        eb.addField(getString("stats_title"), getString(
                "stats_content",
                StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_MESSAGE).getEffect()),
                StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_DAY).getEffect()),
                StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_VC).getEffect()),
                StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_TREASURE).getEffect()),
                buyableRoles.size() > 0 && roleLvl > 0 && roleLvl <= buyableRoles.size() ? buyableRoles.get(roleLvl - 1).getMentionTag() : "**-**",
                StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_SURVEY).getEffect()),
                fisheryMemberBean.getGuildBean().hasFisheryCoinsGivenLimit() ? StringUtil.numToString(fisheryMemberBean.getCoinsGivenMax()) : "∞"
        ), false);

        return eb;
    }

}