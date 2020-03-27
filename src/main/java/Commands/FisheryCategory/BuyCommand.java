package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import General.*;
import General.Fishing.FishingSlot;
import General.Fishing.FishingProfile;
import MySQL.DBServerOld;
import MySQL.DBUser;
import MySQL.Server.DBServer;
import MySQL.Server.ServerBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "buy",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
        emoji = "\uD83D\uDCE5",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/shopping-icon.png",
        executable = true,
        aliases = {"shop", "upgrade"}
)
public class BuyCommand extends Command implements onNavigationListener {

    public BuyCommand() {
        super();
    }

    private ArrayList<Role> roles;
    private FishingProfile fishingProfile;
    private int numberReactions = 0;
    private Server server;
    private ServerBean serverBean;

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            FisheryStatus status = DBServer.getInstance().getServerBean(event.getServer().get().getId()).getFisheryStatus();
            if (status == FisheryStatus.ACTIVE) {
                serverBean = DBServer.getInstance().getServerBean(event.getServer().get().getId());
                server = event.getServer().get();
                fishingProfile = DBUser.getFishingProfile(server, event.getMessage().getUserAuthor().get());
                roles = DBServerOld.getPowerPlantRolesFromServer(server);

                checkRolesWithLog(roles, null);

                return Response.TRUE;
            } else {
                setState(1);
                removeNavigation();
                return Response.FALSE;
            }
        }
        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                deleteNavigationMessage();
                return false;
            } else if (i >= 0) {
                synchronized(event.getUser())  {
                    fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getUser());
                    roles = DBServerOld.getPowerPlantRolesFromServer(event.getServer().get());

                    //Skip treasure chests if they aren't active
                    if (i >= FishingCategoryInterface.PER_TREASURE && !serverBean.isFisheryTreasureChests()) i++;

                    //Skip role if it shouldn't be bought
                    if (i >= FishingCategoryInterface.ROLE &&
                            (fishingProfile.find(FishingCategoryInterface.ROLE).getLevel() >= roles.size() || !Tools.canManageRole(roles.get(fishingProfile.find(FishingCategoryInterface.ROLE).getLevel())))
                    ) i++;
                    FishingSlot slot = fishingProfile.find(i);

                    long price = slot.getPrice();
                    if (slot.getId() == FishingCategoryInterface.ROLE) price = calculateRolePrice(slot);
                    if (fishingProfile.getCoins() >= price) {
                        DBUser.addFishingValues(getLocale(), event.getServer().get(), event.getUser(), 0, -price, true);
                        slot.levelUp();
                        DBUser.updatePowerUpLevel(event.getServer().get(), event.getUser(), slot.getId(), slot.getLevel());
                        fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getUser());

                        if (slot.getId() == FishingCategoryInterface.ROLE) {
                            if (slot.getLevel() - 1 > 0 && serverBean.isFisherySingleRoles()) {
                                roles.get(slot.getLevel() - 2).removeUser(event.getUser()).get();
                            }
                            roles.get(slot.getLevel() - 1).addUser(event.getUser()).get();

                            Optional<ServerTextChannel> announcementChannelOpt = serverBean.getFisheryAnnouncementChannel();
                            if (announcementChannelOpt.isPresent() && PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getTrigger(), announcementChannelOpt.get(), Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS)) {
                                String announcementText = getString("newrole", event.getUser().getMentionTag(), roles.get(slot.getLevel() - 1).getName(), String.valueOf(slot.getLevel()));
                                announcementChannelOpt.get().sendMessage(Tools.defuseMassPing(announcementText)).get();
                            }
                        }

                        setLog(LogStatus.SUCCESS, getString("levelup", getString("product_" + slot.getId() + "_0")));
                        return true;
                    } else {
                        setLog(LogStatus.FAILURE, getString("notenough"));
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {

        switch (state) {
            case 0:
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);

                eb.addField(getString("beginning_title"), getString("beginning"));

                StringBuilder description;
                numberReactions = 0;

                int i = 0;
                for(FishingSlot slot: fishingProfile.getSlots()) {
                    description = new StringBuilder();
                    if (
                            (slot.getId() != FishingCategoryInterface.ROLE ||
                            (slot.getLevel() < roles.size() &&
                                    Tools.canManageRole(roles.get(slot.getLevel())))) &&
                            (slot.getId() != FishingCategoryInterface.PER_TREASURE || serverBean.isFisheryTreasureChests())
                    ) {
                        String productDescription = "???";
                        long price = slot.getPrice();
                        if (slot.getId() != FishingCategoryInterface.ROLE)
                            productDescription = getString("product_des_" + slot.getId(), Tools.numToString(getLocale(), slot.getDeltaEffect()));
                        else if (roles.get(slot.getLevel()) != null) {
                            price = calculateRolePrice(slot);
                            productDescription = getString("product_des_" + slot.getId(), roles.get(slot.getLevel()).getMentionTag());
                        }
                        description.append(getString("product", LetterEmojis.LETTERS[i], FishingCategoryInterface.PRODUCT_EMOJIS[slot.getId()], getString("product_" + slot.getId() + "_0"), String.valueOf(slot.getLevel()), Tools.numToString(getLocale(), price), productDescription));

                        numberReactions++;
                        eb.addField(Tools.getEmptyCharacter(), description.toString());
                        i++;
                    }
                }

                int roleLvl = fishingProfile.getSlots().get(FishingCategoryInterface.ROLE).getLevel();
                eb.addField(Tools.getEmptyCharacter(),
                        getString("status",
                                Tools.numToString(getLocale(), fishingProfile.getFish()),
                                Tools.numToString(getLocale(), fishingProfile.getCoins()),
                                Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_MESSAGE)),
                                Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_DAY)),
                                Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_VC)),
                                Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_TREASURE)),
                                roles.size() > 0 && roleLvl > 0 && roleLvl <= roles.size() ? roles.get(roleLvl - 1).getMentionTag() : "**-**",
                                Tools.numToString(getLocale(), fishingProfile.getEffect(FishingCategoryInterface.PER_SURVEY))
                        )
                );
                return eb;

            case 1:
                return EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title"));
        }
        return null;
    }

    private long calculateRolePrice(FishingSlot slot) throws ExecutionException {
        return FisheryCommand.getFisheryRolePrice(server, roles, slot.getLevel());
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return numberReactions;
    }
}
