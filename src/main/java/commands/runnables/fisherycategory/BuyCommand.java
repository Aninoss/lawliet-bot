package commands.runnables.fisherycategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListenerOld;
import commands.runnables.FisheryAbstract;
import constants.*;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildBean;
import mysql.modules.fisheryusers.FisheryMemberBean;
import mysql.modules.fisheryusers.FisheryMemberPowerUpBean;
import mysql.modules.server.DBServer;
import mysql.modules.server.GuildBean;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "buy",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        emoji = "📥",
        executableWithoutArgs = true,
        aliases = { "shop", "upgrade", "invest", "levelup", "b" }
)
public class BuyCommand extends FisheryAbstract implements OnNavigationListenerOld {

    private FisheryMemberBean fisheryMemberBean;
    private FisheryGuildBean fisheryGuildBean;
    private int numberReactions = 0;
    private Server server;
    private GuildBean guildBean;

    public BuyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceivedSuccessful(MessageCreateEvent event, String followedString) throws Throwable {
        guildBean = DBServer.getInstance().retrieve(event.getServer().get().getId());
        server = event.getServer().get();
        fisheryMemberBean = DBFishery.getInstance().retrieve(server.getId()).getUserBean(event.getMessageAuthor().getId());
        fisheryGuildBean = fisheryMemberBean.getFisheryServerBean();

        checkRolesWithLog(fisheryGuildBean.getRoles(), null);

        if (followedString.length() > 0) {
            String letters = StringUtil.filterLettersFromString(followedString).toLowerCase().replace(" ", "");
            long numbers = StringUtil.filterLongFromString(followedString);

            int i = getI(letters);

            long amount = 1;
            if (numbers != -1) {
                if (numbers >= 1 && numbers <= 100) {
                    amount = numbers;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number2", "1", "100"));
                    return true;
                }
            }

            if (i >= 0) {
                for(int j = 0; j < amount; j++) {
                    if (!buy(i, event.getMessage().getUserAuthor().get(), false)) {
                        break;
                    }
                }

                return true;
            }

            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), followedString));
        }

        return true;
    }

    private int getI(String letters) {
        switch (letters) {
            case "fishingrod":
            case "rod":
            case "message":
            case "messages":
                return FisheryCategoryInterface.PER_MESSAGE;

            case "fishingrobot":
            case "robot":
            case "fishingbot":
            case "bot":
            case "daily":
            case "dailies":
                return FisheryCategoryInterface.PER_DAY;

            case "fishingnet":
            case "net":
            case "vc":
            case "voicechannel":
            case "voicechannels":
                return FisheryCategoryInterface.PER_VC;

            case "metaldetector":
            case "treasurechest":
            case "treasurechests":
            case "chest":
            case "chests":
                return FisheryCategoryInterface.PER_TREASURE;

            case "role":
            case "roles":
            case "buyablerole":
            case "buyableroles":
            case "fisheryrole":
            case "fisheryroles":
                return FisheryCategoryInterface.ROLE;

            case "survey":
            case "surveys":
                return FisheryCategoryInterface.PER_SURVEY;

            default:
                return -1;
        }
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable { return null; }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                removeNavigationWithMessage();
                return false;
            } else if (i >= 0) {
                buy(i, event.getUser().get(), true);
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean buy(int i, User user, boolean transferableSlots) throws ExecutionException, InterruptedException {
        synchronized(user)  {
            List<Role> roles = fisheryGuildBean.getRoles();

            boolean canUseTreasureChests = guildBean.isFisheryTreasureChests();
            boolean canUseRoles = fisheryMemberBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel() < fisheryGuildBean.getRoleIds().size() && BotPermissionUtil.canYouManageRole(roles.get(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel()));

            if (transferableSlots) {
                if (i >= FisheryCategoryInterface.PER_TREASURE && !canUseTreasureChests) i++;
                if (i >= FisheryCategoryInterface.ROLE && !canUseRoles) i++;
            } else {
                if (i == FisheryCategoryInterface.PER_TREASURE && !canUseTreasureChests) return false;
                if (i == FisheryCategoryInterface.ROLE && !canUseRoles) return false;
            }
            if (i > 5 || i < 0) return false;

            FisheryMemberPowerUpBean slot = fisheryMemberBean.getPowerUp(i);

            long price = slot.getPrice();
            if (slot.getPowerUpId() == FisheryCategoryInterface.ROLE) {
                price = calculateRolePrice(slot);
            }

            if (fisheryMemberBean.getCoins() >= price) {
                upgrade(slot, price, roles, user);
                setLog(LogStatus.SUCCESS, getString("levelup", getString("product_" + slot.getPowerUpId() + "_0")));
                return true;
            } else {
                setLog(LogStatus.FAILURE, getString("notenough"));
                return false;
            }
        }
    }

    private void upgrade(FisheryMemberPowerUpBean slot, long price, List<Role> roles, User user) throws ExecutionException, InterruptedException {
        fisheryMemberBean.changeValues(0, -price);
        fisheryMemberBean.levelUp(slot.getPowerUpId());

        if (slot.getPowerUpId() == FisheryCategoryInterface.ROLE) {
            Role role = roles.get(slot.getLevel() - 1);
            if (role.getCurrentCachedInstance().isPresent()) {
                role.addUser(user).get();
                if (slot.getLevel() > 1) {
                    Server server = roles.get(0).getServer();
                    if (guildBean.isFisherySingleRoles()) {
                        for (int j = slot.getLevel() - 2; j >= 0; j--) {
                            if (user.getRoles(server).contains(roles.get(j)))
                                roles.get(j).removeUser(user).exceptionally(ExceptionLogger.get());
                        }
                    } else {
                        for (int j = slot.getLevel() - 2; j >= 0; j--) {
                            if (!user.getRoles(server).contains(roles.get(j)))
                                roles.get(j).addUser(user).exceptionally(ExceptionLogger.get());
                        }
                    }
                }

                Optional<ServerTextChannel> announcementChannelOpt = guildBean.getFisheryAnnouncementChannel();
                if (announcementChannelOpt.isPresent() && PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), announcementChannelOpt.get(), PermissionDeprecated.SEND_MESSAGES)) {
                    String announcementText = getString("newrole", user.getMentionTag(), StringUtil.escapeMarkdown(roles.get(slot.getLevel() - 1).getName()), String.valueOf(slot.getLevel()));
                    announcementChannelOpt.get().sendMessage(StringUtil.defuseMassPing(announcementText)).exceptionally(ExceptionLogger.get());
                }
            }
        }
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        List<Role> roles = fisheryGuildBean.getRoles();

        switch (state) {
            case 0:
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);

                eb.addField(getString("beginning_title"), getString("beginning"));

                StringBuilder description;
                numberReactions = 0;

                int i = 0;
                for(FisheryMemberPowerUpBean slot : fisheryMemberBean.getPowerUpMap().values()) {
                    description = new StringBuilder();
                    if (
                            (slot .getPowerUpId() != FisheryCategoryInterface.ROLE ||
                            (slot.getLevel() < fisheryGuildBean.getRoleIds().size() &&
                                    BotPermissionUtil.canYouManageRole(roles.get(slot.getLevel())))) &&
                            (slot.getPowerUpId() != FisheryCategoryInterface.PER_TREASURE || guildBean.isFisheryTreasureChests())
                    ) {
                        String productDescription = "???";
                        long price = slot.getPrice();
                        if (slot.getPowerUpId() != FisheryCategoryInterface.ROLE) {
                            productDescription = getString("product_des_" + slot.getPowerUpId(), StringUtil.numToString(slot.getDeltaEffect()));
                        } else if (roles.get(slot.getLevel()) != null) {
                            price = calculateRolePrice(slot);
                            productDescription = getString("product_des_" + slot.getPowerUpId(), roles.get(slot.getLevel()).getMentionTag());
                        }
                        description.append(getString("product", LetterEmojis.LETTERS[i], FisheryCategoryInterface.PRODUCT_EMOJIS[slot.getPowerUpId()], getString("product_" + slot.getPowerUpId() + "_0"), String.valueOf(slot.getLevel()), StringUtil.numToString(price), productDescription));

                        numberReactions++;
                        eb.addField(Emojis.EMPTY_EMOJI, description.toString());
                        i++;
                    }
                }

                int roleLvl = fisheryMemberBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel();

                String status = getString("status",
                        StringUtil.numToString(fisheryMemberBean.getFish()),
                        StringUtil.numToString(fisheryMemberBean.getCoins()),
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_MESSAGE).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_DAY).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_VC).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_TREASURE).getEffect()),
                        roles.size() > 0 && roleLvl > 0 && roleLvl <= roles.size() ? roles.get(roleLvl - 1).getMentionTag() : "**-**",
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_SURVEY).getEffect()),
                        fisheryMemberBean.getGuildBean().hasFisheryCoinsGivenLimit() ? StringUtil.numToString(fisheryMemberBean.getCoinsGivenMax()) : "∞"
                );

                eb.addField(Emojis.EMPTY_EMOJI, StringUtil.shortenStringLine(status, 1024));
                return eb;

            case 1:
                return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title"));

            default:
                return null;
        }
    }

    private long calculateRolePrice(FisheryMemberPowerUpBean slot) throws ExecutionException {
        return Fishery.getFisheryRolePrice(server, fisheryGuildBean.getRoleIds(), slot.getLevel());
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return numberReactions;
    }
}
