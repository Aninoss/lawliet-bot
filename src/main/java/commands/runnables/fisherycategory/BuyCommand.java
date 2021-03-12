package commands.runnables.fisherycategory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.FisheryCategoryInterface;
import constants.LogStatus;
import constants.Response;
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
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "buy",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "📥",
        executableWithoutArgs = true,
        aliases = { "shop", "upgrade", "invest", "levelup", "b" }
)
public class BuyCommand extends NavigationAbstract implements FisheryInterface {

    private FisheryMemberBean fisheryMemberBean;
    private FisheryGuildBean fisheryGuildBean;
    private GuildBean guildBean;

    public BuyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        fisheryMemberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(event.getMember().getIdLong());
        fisheryGuildBean = fisheryMemberBean.getFisheryServerBean();

        checkRolesWithLog(null, fisheryGuildBean.getRoles());
        if (args.length() > 0) {
            String letters = StringUtil.filterLettersFromString(args).toLowerCase().replace(" ", "");
            long numbers = StringUtil.filterLongFromString(args);

            int i = getI(letters);

            long amount = 1;
            if (numbers != -1) {
                if (numbers >= 1 && numbers <= 100) {
                    amount = numbers;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number2", "1", "100"));
                    registerNavigationListener(getUpgradablePowerUpBeans().size());
                    return true;
                }
            }

            if (i >= 0) {
                for (int j = 0; j < amount; j++) {
                    if (!buy(i, event.getMember(), false)) {
                        break;
                    }
                }

                registerNavigationListener(getUpgradablePowerUpBeans().size());
                return true;
            }

            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
        }

        registerNavigationListener(getUpgradablePowerUpBeans().size());
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
    public Response controllerMessage(GuildMessageReceivedEvent event, String inputString, int state) throws Throwable {
        return null;
    }

    @Override
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                removeNavigationWithMessage();
                return false;
            } else if (i >= 0) {
                buy(i, event.getMember(), true);
                return true;
            }
            return false;
        }
        return false;
    }

    private synchronized boolean buy(int i, Member member, boolean transferableSlots) {
        List<Role> roles = fisheryGuildBean.getRoles();

        boolean canUseTreasureChests = guildBean.isFisheryTreasureChests();
        boolean canUseRoles = fisheryMemberBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel() < fisheryGuildBean.getRoleIds().size() &&
                BotPermissionUtil.can(member.getGuild(), Permission.MANAGE_ROLES) &&
                member.canInteract(roles.get(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel()));

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
            upgrade(slot, price, roles, member);
            setLog(LogStatus.SUCCESS, getString("levelup", getString("product_" + slot.getPowerUpId() + "_0")));
            return true;
        } else {
            setLog(LogStatus.FAILURE, getString("notenough"));
            return false;
        }
    }

    private void upgrade(FisheryMemberPowerUpBean slot, long price, List<Role> roles, Member member) {
        fisheryMemberBean.changeValues(0, -price);
        fisheryMemberBean.levelUp(slot.getPowerUpId());

        if (slot.getPowerUpId() == FisheryCategoryInterface.ROLE) {
            Role role = roles.get(slot.getLevel() - 1);
            role.getGuild().addRoleToMember(member, role).queue();
            if (slot.getLevel() > 1) {
                if (guildBean.isFisherySingleRoles()) {
                    for (int j = slot.getLevel() - 2; j >= 0; j--) {
                        if (member.getRoles().contains(roles.get(j))) {
                            member.getGuild().removeRoleFromMember(member, roles.get(j)).queue();
                        }
                    }
                } else {
                    for (int j = slot.getLevel() - 2; j >= 0; j--) {
                        if (!member.getRoles().contains(roles.get(j))) {
                            member.getGuild().addRoleToMember(member, roles.get(j)).queue();
                        }
                    }
                }
            }

            Optional<TextChannel> announcementChannelOpt = guildBean.getFisheryAnnouncementChannel();
            if (announcementChannelOpt.isPresent() && PermissionCheckRuntime.getInstance().botHasPermission(getLocale(), getClass(), announcementChannelOpt.get(), Permission.MESSAGE_WRITE)) {
                String announcementText = getString("newrole", member.getUser().getAsMention(), StringUtil.escapeMarkdown(roles.get(slot.getLevel() - 1).getName()), String.valueOf(slot.getLevel()));
                announcementChannelOpt.get().sendMessage(announcementText).queue();
            }
        }
    }

    @Override
    public EmbedBuilder draw(int state) {
        List<Role> roles = fisheryGuildBean.getRoles();

        switch (state) {
            case 0:
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
                eb.addField(getString("beginning_title"), getString("beginning"), false);

                StringBuilder description;
                int i = 0;

                for (FisheryMemberPowerUpBean slot : getUpgradablePowerUpBeans()) {
                    description = new StringBuilder();
                    String productDescription = "???";
                    long price = slot.getPrice();
                    if (slot.getPowerUpId() != FisheryCategoryInterface.ROLE) {
                        productDescription = getString("product_des_" + slot.getPowerUpId(), StringUtil.numToString(slot.getDeltaEffect()));
                    } else if (roles.get(slot.getLevel()) != null) {
                        price = calculateRolePrice(slot);
                        productDescription = getString("product_des_" + slot.getPowerUpId(), roles.get(slot.getLevel()).getAsMention());
                    }
                    description.append(getString("product", Emojis.LETTERS[i], FisheryCategoryInterface.PRODUCT_EMOJIS[slot.getPowerUpId()], getString("product_" + slot.getPowerUpId() + "_0"), String.valueOf(slot.getLevel()), StringUtil.numToString(price), productDescription));

                    eb.addField(Emojis.EMPTY_EMOJI, description.toString(), false);
                    i++;
                }

                int roleLvl = fisheryMemberBean.getPowerUp(FisheryCategoryInterface.ROLE).getLevel();

                String status = getString(
                        "status",
                        StringUtil.numToString(fisheryMemberBean.getFish()),
                        StringUtil.numToString(fisheryMemberBean.getCoins()),
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_MESSAGE).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_DAY).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_VC).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_TREASURE).getEffect()),
                        roles.size() > 0 && roleLvl > 0 && roleLvl <= roles.size() ? roles.get(roleLvl - 1).getAsMention() : "**-**",
                        StringUtil.numToString(fisheryMemberBean.getPowerUp(FisheryCategoryInterface.PER_SURVEY).getEffect()),
                        fisheryMemberBean.getGuildBean().hasFisheryCoinsGivenLimit() ? StringUtil.numToString(fisheryMemberBean.getCoinsGivenMax()) : "∞"
                );

                eb.addField(Emojis.EMPTY_EMOJI, StringUtil.shortenStringLine(status, 1024), false);
                return eb;

            case 1:
                return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title"));

            default:
                return null;
        }
    }

    private List<FisheryMemberPowerUpBean> getUpgradablePowerUpBeans() {
        List<Role> roles = fisheryGuildBean.getRoles();
        return fisheryMemberBean.getPowerUpMap().values().stream()
                .filter(slot -> (slot.getPowerUpId() != FisheryCategoryInterface.ROLE ||
                        (slot.getLevel() < fisheryGuildBean.getRoleIds().size() &&
                                BotPermissionUtil.can(getGuild().get(), Permission.MANAGE_ROLES) &&
                                getGuild().get().getSelfMember().canInteract(roles.get(slot.getLevel())))) &&
                        (slot.getPowerUpId() != FisheryCategoryInterface.PER_TREASURE || guildBean.isFisheryTreasureChests())
                ).collect(Collectors.toList());
    }

    private long calculateRolePrice(FisheryMemberPowerUpBean slot) {
        return Fishery.getFisheryRolePrice(getGuild().get(), fisheryGuildBean.getRoleIds(), slot.getLevel());
    }

}
