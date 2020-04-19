package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import Core.*;
import Core.Tools.StringTools;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.FisheryUsers.FisheryUserPowerUpBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "buy",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        emoji = "\uD83D\uDCE5",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat/128/shopping-icon.png",
        executable = true,
        aliases = {"shop", "upgrade"}
)
public class BuyCommand extends Command implements OnNavigationListener {

    private FisheryUserBean fisheryUserBean;
    private FisheryServerBean fisheryServerBean;
    private int numberReactions = 0;
    private Server server;
    private ServerBean serverBean;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        FisheryStatus status = DBServer.getInstance().getBean(event.getServer().get().getId()).getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
            server = event.getServer().get();
            fisheryUserBean = DBFishery.getInstance().getBean(server.getId()).getUserBean(event.getMessageAuthor().getId());
            fisheryServerBean = fisheryUserBean.getFisheryServerBean();

            checkRolesWithLog(fisheryServerBean.getRoles(), null);
            return true;
        } else {
            setState(1);
            removeNavigation();
            return false;
        }
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable { return null; }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                deleteNavigationMessage();
                return false;
            } else if (i >= 0) {
                synchronized(event.getUser())  {
                    //Skip treasure chests if they aren't active
                    if (i >= FisheryCategoryInterface.PER_TREASURE && !serverBean.isFisheryTreasureChests()) i++;

                    List<Role> roles = fisheryServerBean.getRoles();

                    //Skip role if it shouldn't be bought
                    if (i >= FisheryCategoryInterface.ROLE &&
                            (fisheryUserBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel() >= fisheryServerBean.getRoleIds().size() || !PermissionCheck.canYouManageRole(roles.get(fisheryUserBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel())))
                    ) i++;
                    FisheryUserPowerUpBean slot = fisheryUserBean.getPowerUp(i);

                    long price = slot.getPrice();
                    if (slot.getPowerUpId() == FisheryCategoryInterface.ROLE) price = calculateRolePrice(slot);
                    if (fisheryUserBean.getCoins() >= price) {
                        fisheryUserBean.changeValues(0, -price);
                        fisheryUserBean.levelUp(slot.getPowerUpId());

                        if (slot.getPowerUpId() == FisheryCategoryInterface.ROLE) {
                            roles.get(slot.getLevel() - 1).addUser(event.getUser()).get();
                            if (slot.getLevel() > 1) {
                                if (serverBean.isFisherySingleRoles())
                                    for(int j = slot.getLevel() - 2; j >= 0; j--) {
                                        if (roles.get(j).getUsers().contains(event.getUser())) roles.get(j).removeUser(event.getUser());
                                    }
                                else
                                    for(int j = slot.getLevel() - 2; j >= 0; j--) {
                                        if (!roles.get(j).getUsers().contains(event.getUser())) roles.get(j).addUser(event.getUser());
                                    }
                            }

                            Optional<ServerTextChannel> announcementChannelOpt = serverBean.getFisheryAnnouncementChannel();
                            if (announcementChannelOpt.isPresent() && PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), announcementChannelOpt.get(), Permission.SEND_MESSAGES | Permission.EMBED_LINKS)) {
                                String announcementText = getString("newrole", event.getUser().getMentionTag(), roles.get(slot.getLevel() - 1).getName(), String.valueOf(slot.getLevel()));
                                announcementChannelOpt.get().sendMessage(StringTools.defuseMassPing(announcementText)).get();
                            }
                        }

                        setLog(LogStatus.SUCCESS, getString("levelup", getString("product_" + slot.getPowerUpId() + "_0")));
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
        List<Role> roles = fisheryServerBean.getRoles();

        switch (state) {
            case 0:
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);

                eb.addField(getString("beginning_title"), getString("beginning"));

                StringBuilder description;
                numberReactions = 0;

                int i = 0;
                for(FisheryUserPowerUpBean slot : fisheryUserBean.getPowerUpMap().values()) {
                    description = new StringBuilder();
                    if (
                            (slot .getPowerUpId() != FisheryCategoryInterface.ROLE ||
                            (slot.getLevel() < fisheryServerBean.getRoleIds().size() &&
                                    PermissionCheck.canYouManageRole(roles.get(slot.getLevel())))) &&
                            (slot.getPowerUpId() != FisheryCategoryInterface.PER_TREASURE || serverBean.isFisheryTreasureChests())
                    ) {
                        String productDescription = "???";
                        long price = slot.getPrice();
                        if (slot.getPowerUpId() != FisheryCategoryInterface.ROLE)
                            productDescription = getString("product_des_" + slot.getPowerUpId(), StringTools.numToString(getLocale(), slot.getDeltaEffect()));
                        else if (roles.get(slot.getLevel()) != null) {
                            price = calculateRolePrice(slot);
                            productDescription = getString("product_des_" + slot.getPowerUpId(), roles.get(slot.getLevel()).getMentionTag());
                        }
                        description.append(getString("product", LetterEmojis.LETTERS[i], FisheryCategoryInterface.PRODUCT_EMOJIS[slot.getPowerUpId()], getString("product_" + slot.getPowerUpId() + "_0"), String.valueOf(slot.getLevel()), StringTools.numToString(getLocale(), price), productDescription));

                        numberReactions++;
                        eb.addField(Settings.EMPTY_EMOJI, description.toString());
                        i++;
                    }
                }

                int roleLvl = fisheryUserBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel();

                eb.addField(Settings.EMPTY_EMOJI,
                        getString("status",
                                StringTools.numToString(getLocale(), fisheryUserBean.getFish()),
                                StringTools.numToString(getLocale(), fisheryUserBean.getCoins()),
                                StringTools.numToString(getLocale(), fisheryUserBean.getPowerUp(FisheryCategoryInterface.PER_MESSAGE).getEffect()),
                                StringTools.numToString(getLocale(), fisheryUserBean.getPowerUp(FisheryCategoryInterface.PER_DAY).getEffect()),
                                StringTools.numToString(getLocale(), fisheryUserBean.getPowerUp(FisheryCategoryInterface.PER_VC).getEffect()),
                                StringTools.numToString(getLocale(), fisheryUserBean.getPowerUp(FisheryCategoryInterface.PER_TREASURE).getEffect()),
                                roles.size() > 0 && roleLvl > 0 && roleLvl <= roles.size() ? roles.get(roleLvl - 1).getMentionTag() : "**-**",
                                StringTools.numToString(getLocale(), fisheryUserBean.getPowerUp(FisheryCategoryInterface.PER_SURVEY).getEffect())
                        )
                );
                return eb;

            case 1:
                return EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title"));
        }
        return null;
    }

    private long calculateRolePrice(FisheryUserPowerUpBean slot) throws ExecutionException {
        return FisheryCommand.getFisheryRolePrice(server, fisheryServerBean.getRoleIds(), slot.getLevel());
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return numberReactions;
    }
}
