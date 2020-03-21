package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.FishingCategoryInterface;
import Constants.Permission;
import Constants.PowerPlantStatus;
import General.EmbedFactory;
import General.Fishing.FishingProfile;
import General.Fishing.FishingSlot;
import General.Mention.MentionFinder;
import General.TextManager;
import General.Tools;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "gear",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
        thumbnail = "http://icons.iconarchive.com/icons/thegirltyler/brand-camp/128/Fishing-Worm-icon.png",
        emoji = "\uD83C\uDFA3",
        executable = true,
        aliases = {"equip", "equipment"}
)
public class GearCommand extends Command implements onRecievedListener {

    public GearCommand() {
        super();
    }

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws SQLException, IOException, ExecutionException, InterruptedException {
        PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
        if (status == PowerPlantStatus.ACTIVE) {
            Server server = event.getServer().get();
            Message message = event.getMessage();
            ArrayList<User> list = MentionFinder.getUsers(message,followedString).getList();
            if (list.size() > 5) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                        TextManager.getString(getLocale(),TextManager.GENERAL,"too_many_users"))).get();
                return false;
            }
            boolean userMentioned = true;
            boolean userBefore = list.size() > 0;
            for(User user: new ArrayList<>(list)) {
                if (user.isBot()) list.remove(user);
            }
            if (list.size() == 0) {
                if (userBefore) {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(),TextManager.COMMANDS,"acc_nobot"))).get();
                    return false;
                } else {
                    list.add(message.getUserAuthor().get());
                    userMentioned = false;
                }
            }

            ArrayList<Role> buyableRoles = DBServer.getPowerPlantRolesFromServer(server);
            for(User user: list) {
                FishingProfile fishingProfile = DBUser.getFishingProfile(server, user);
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("desc", Tools.numToString(getLocale(), fishingProfile.getFish()), Tools.numToString(getLocale(), fishingProfile.getCoins())));
                if (eb != null) {
                    eb.setTitle("");
                    eb.setAuthor(getString("author", user.getDisplayName(server)), "", user.getAvatar());
                    eb.setThumbnail(user.getAvatar());

                    //Gear
                    StringBuilder gearString = new StringBuilder();
                    for(FishingSlot slot: fishingProfile.getSlots()) {
                        gearString.append(getString("gear_slot",
                                FishingCategoryInterface.PRODUCT_EMOJIS[slot.getId()],
                                TextManager.getString(getLocale(), TextManager.COMMANDS, "buy_product_" + slot.getId() + "_0"),
                                String.valueOf(slot.getLevel())
                        )).append("\n");
                    }
                    eb.addField(getString("gear_title"), gearString.toString(), false);

                    int roleLvl = fishingProfile.getSlots().get(FishingCategoryInterface.ROLE).getLevel();
                    eb.addField(getString("stats_title"), getString("stats_content",
                            Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_MESSAGE)),
                            Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_DAY)),
                            Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_VC)),
                            Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_TREASURE)),
                            buyableRoles.size() > 0 && roleLvl > 0 && roleLvl <= buyableRoles.size() ? buyableRoles.get(roleLvl - 1).getMentionTag() : "**-**",
                            Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_SURVEY))
                    ), false);

                    if (!userMentioned)
                        eb.setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "mention_optional"));
                    event.getChannel().sendMessage(eb).get();
                }
            }
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }
    }
}
