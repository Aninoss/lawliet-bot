package Commands.PowerPlant;

import CommandListeners.onNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import General.*;
import General.Fishing.FishingSlot;
import General.Fishing.FishingProfile;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;

public class BuyCommand extends Command implements onNavigationListener {
    private ArrayList<Role> roles;
    private FishingProfile fishingProfile;
    private int numberReactions = 0;
    private boolean singleRole;

    public BuyCommand() {
        super();
        trigger = "buy";
        privateUse = false;
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        emoji = "\uD83D\uDCE5";
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/shopping-icon.png";
        executable = true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, boolean firstTime) throws Throwable {
        if (firstTime) {
            PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
            if (status == PowerPlantStatus.ACTIVE) {
                fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getMessage().getUserAuthor().get());
                roles = DBServer.getPowerPlantRolesFromServer(event.getServer().get());
                singleRole = DBServer.getPowerPlantSingleRoleFromServer(event.getServer().get());
                return Response.TRUE;
            } else {
                state = 1;
                removeNavigation();
                return Response.FALSE;
            }
        }
        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                deleteNavigationMessage();
                return false;
            } else if (i >= 0) {
                synchronized(event.getUser())  {
                    fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getUser());
                    roles = DBServer.getPowerPlantRolesFromServer(event.getServer().get());

                    if (i >= FishingCategoryInterface.ROLE && fishingProfile.find(FishingCategoryInterface.ROLE).getLevel() >= roles.size()) i++;
                    FishingSlot slot = fishingProfile.find(i);

                    if (fishingProfile.getCoins() >= slot.getPrice()) {
                        EmbedBuilder eb = DBUser.addFishingValues(locale, event.getServer().get(), event.getUser(), 0, -slot.getPrice());
                        DBUser.updatePowerUpLevel(event.getServer().get(), event.getUser(), slot.getId(), slot.getLevel() + 1);
                        fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getUser());

                        if (slot.getId() == FishingCategoryInterface.ROLE) {
                            if (slot.getLevel() > 0 && singleRole) {
                                roles.get(slot.getLevel() - 1).removeUser(event.getUser()).get();
                            }
                            roles.get(slot.getLevel()).addUser(event.getUser()).get();

                            ServerTextChannel announcementChannel = DBServer.getPowerPlantAnnouncementChannelFromServer(event.getServer().get());
                            if (announcementChannel != null) {
                                announcementChannel.sendMessage(getString("newrole", event.getUser().getMentionTag(), roles.get(slot.getLevel()).getName(), String.valueOf(slot.getLevel() + 1))).get();
                            }
                        }

                        setLog(LogStatus.SUCCESS, getString("levelup", getString("product_" + slot.getId() + "_0")));
                        event.getChannel().sendMessage(eb).get();
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
    public EmbedBuilder draw(DiscordApi api) throws Throwable {
        switch (state) {
            case 0:
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);

                eb.addField(getString("beginning_title"), getString("beginning"));

                StringBuilder description;
                numberReactions = 0;

                int i = 0;
                for(FishingSlot slot: fishingProfile.getSlots()) {
                    description = new StringBuilder();
                    if (slot.getId() != FishingCategoryInterface.ROLE || slot.getLevel() < roles.size()) {
                        String productDescription = "???";
                        if (slot.getId() != FishingCategoryInterface.ROLE)
                            productDescription = getString("product_des_" + slot.getId(), Tools.numToString(locale, slot.getDeltaEffect()));
                        else if (roles.get(slot.getLevel()) != null)
                            productDescription = getString("product_des_" + slot.getId(), roles.get(slot.getLevel()).getMentionTag());
                        description.append(getString("product", LetterEmojis.LETTERS[i], FishingCategoryInterface.PRODUCT_EMOJIS[slot.getId()], getString("product_" + slot.getId() + "_0"), String.valueOf(slot.getLevel()), Tools.numToString(locale, slot.getPrice()), productDescription));

                        numberReactions++;
                        eb.addField(Tools.getEmptyCharacter(), description.toString());
                        i++;
                    }
                }

                eb.addField(Tools.getEmptyCharacter(),
                        getString("status",
                                Tools.numToString(locale, fishingProfile.getFish()),
                                Tools.numToString(locale, fishingProfile.getCoins()),
                                Tools.numToString(locale, fishingProfile.getEffect(FishingCategoryInterface.PER_MESSAGE)),
                                Tools.numToString(locale, fishingProfile.getEffect(FishingCategoryInterface.PER_DAY)),
                                Tools.numToString(locale, fishingProfile.getEffect(FishingCategoryInterface.PER_VC)),
                                Tools.numToString(locale, fishingProfile.getEffect(FishingCategoryInterface.PER_TREASURE)),
                                Tools.numToString(locale, fishingProfile.getEffect(FishingCategoryInterface.PER_SURVEY))
                        )
                );
                return eb;

            case 1:
                return EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", prefix), TextManager.getString(locale, TextManager.GENERAL, "fishing_notactive_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return numberReactions;
    }
}
