package commands.runnables.fisherycategory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.FisheryGear;
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
import mysql.modules.fisheryusers.FisheryMemberGearBean;
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

        checkRolesWithLog(event.getGuild(), fisheryGuildBean.getRoles());
        if (args.length() > 0) {
            String letters = StringUtil.filterLettersFromString(args).toLowerCase().replace(" ", "");
            long numbers = StringUtil.filterLongFromString(args);
            FisheryGear fisheryGear = getFisheryCategory(letters);

            long amount = 1;
            if (numbers != -1) {
                if (numbers >= 1 && numbers <= 100) {
                    amount = numbers;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "100"));
                    registerNavigationListener(getUpgradableGears().size());
                    return true;
                }
            }

            if (fisheryGear != null) {
                for (int j = 0; j < amount; j++) {
                    if (!buy(fisheryGear, event.getMember(), false)) {
                        break;
                    }
                }

                registerNavigationListener(getUpgradableGears().size());
                return true;
            }

            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
        }

        registerNavigationListener(getUpgradableGears().size());
        return true;
    }

    private FisheryGear getFisheryCategory(String letters) {
        switch (letters) {
            case "fishingrod":
            case "rod":
            case "message":
            case "messages":
                return FisheryGear.MESSAGE;

            case "fishingrobot":
            case "robot":
            case "fishingbot":
            case "bot":
            case "day":
            case "daily":
            case "dailies":
                return FisheryGear.DAILY;

            case "fishingnet":
            case "net":
            case "vc":
            case "voice":
            case "voicechannel":
            case "voicechannels":
                return FisheryGear.VOICE;

            case "metal":
            case "detector":
            case "detectors":
            case "metaldetector":
            case "metaldetectors":
            case "treasurechest":
            case "treasurechests":
            case "chest":
            case "chests":
                return FisheryGear.TREASURE;

            case "role":
            case "roles":
            case "buyablerole":
            case "buyableroles":
            case "fisheryrole":
            case "fisheryroles":
                return FisheryGear.ROLE;

            case "survey":
            case "surveys":
                return FisheryGear.SURVEY;

            case "work":
            case "working":
            case "salary":
                return FisheryGear.WORK;

            default:
                return null;
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
            } else if (i >= 0 && i < FisheryGear.values().length) {
                buy(FisheryGear.values()[i], event.getMember(), true);
                return true;
            }
            return false;
        }
        return false;
    }

    private synchronized boolean buy(FisheryGear fisheryGear, Member member, boolean transferableSlots) {
        List<Role> roles = fisheryGuildBean.getRoles();
        int i = fisheryGear.ordinal();

        boolean canUseTreasureChests = slotIsValid(roles, fisheryMemberBean.getMemberGear(FisheryGear.TREASURE));;
        boolean canUseRoles = slotIsValid(roles, fisheryMemberBean.getMemberGear(FisheryGear.ROLE));;

        if (transferableSlots) {
            if (i >= FisheryGear.TREASURE.ordinal() && !canUseTreasureChests) i++;
            if (i >= FisheryGear.ROLE.ordinal() && !canUseRoles) i++;
        } else {
            if (i == FisheryGear.TREASURE.ordinal() && !canUseTreasureChests) return false;
            if (i == FisheryGear.ROLE.ordinal() && !canUseRoles) return false;
        }
        if (i > FisheryGear.values().length - 1 || i < 0) return false;
        fisheryGear = FisheryGear.values()[i];

        FisheryMemberGearBean slot = fisheryMemberBean.getMemberGear(fisheryGear);

        long price = slot.getPrice();
        if (slot.getGear() == FisheryGear.ROLE) {
            price = calculateRolePrice(slot);
        }

        if (fisheryMemberBean.getCoins() >= price) {
            upgrade(slot, price, roles, member);
            setLog(LogStatus.SUCCESS, getString("levelup", getString("product_" + slot.getGear().ordinal() + "_0")));
            return true;
        } else {
            if (getLog() == null || getLog().isEmpty()) {
                setLog(LogStatus.FAILURE, getString("notenough"));
            }
            return false;
        }
    }

    private void upgrade(FisheryMemberGearBean slot, long price, List<Role> roles, Member member) {
        fisheryMemberBean.changeValues(0, -price);
        fisheryMemberBean.levelUp(slot.getGear());

        if (slot.getGear() == FisheryGear.ROLE) {
            Role role = roles.get(slot.getLevel() - 1);
            role.getGuild().addRoleToMember(member, role)
                    .reason(getCommandLanguage().getTitle())
                    .queue();
            if (slot.getLevel() > 1) {
                if (guildBean.isFisherySingleRoles()) {
                    for (int j = slot.getLevel() - 2; j >= 0; j--) {
                        if (member.getRoles().contains(roles.get(j))) {
                            member.getGuild().removeRoleFromMember(member, roles.get(j))
                                    .reason(getCommandLanguage().getTitle())
                                    .queue();
                        }
                    }
                } else {
                    for (int j = slot.getLevel() - 2; j >= 0; j--) {
                        if (!member.getRoles().contains(roles.get(j))) {
                            member.getGuild().addRoleToMember(member, roles.get(j))
                                    .reason(getCommandLanguage().getTitle())
                                    .queue();
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
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("beginning") + "\n" + Emojis.EMPTY_EMOJI);
                int i = 0;
                for (FisheryMemberGearBean slot : getUpgradableGears()) {
                    String productDescription = "???";
                    long price = slot.getPrice();
                    if (slot.getGear() != FisheryGear.ROLE) {
                        productDescription = getString("product_des_" + slot.getGear().ordinal(), StringUtil.numToString(slot.getDeltaEffect()));
                    } else if (roles.get(slot.getLevel()) != null) {
                        price = calculateRolePrice(slot);
                        productDescription = getString("product_des_" + slot.getGear().ordinal(), roles.get(slot.getLevel()).getAsMention());
                    }

                    eb.addField(
                            getString("product_title", Emojis.LETTERS[i], slot.getGear().getEmoji(), getString("product_" + slot.getGear().ordinal() + "_0"), StringUtil.numToString(slot.getLevel()), StringUtil.numToString(price)),
                            productDescription + "\n" + Emojis.EMPTY_EMOJI,
                            false
                    );
                    i++;
                }

                int roleLvl = fisheryMemberBean.getMemberGear(FisheryGear.ROLE).getLevel();

                String status = getString(
                        "status",
                        StringUtil.numToString(fisheryMemberBean.getFish()),
                        StringUtil.numToString(fisheryMemberBean.getCoins()),
                        StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.MESSAGE).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.DAILY).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.VOICE).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.TREASURE).getEffect()),
                        roles.size() > 0 && roleLvl > 0 && roleLvl <= roles.size() ? roles.get(roleLvl - 1).getAsMention() : "**-**",
                        StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.SURVEY).getEffect()),
                        StringUtil.numToString(fisheryMemberBean.getMemberGear(FisheryGear.WORK).getEffect()),
                        fisheryMemberBean.getGuildBean().hasFisheryCoinsGivenLimit() ? StringUtil.numToString(fisheryMemberBean.getCoinsGivenMax()) : "∞"
                );

                eb.addField(getString("status_title"), StringUtil.shortenStringLine(status, 1024), false);
                return eb;

            case 1:
                return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title"));

            default:
                return null;
        }
    }

    private List<FisheryMemberGearBean> getUpgradableGears() {
        List<Role> roles = fisheryGuildBean.getRoles();
        return fisheryMemberBean.getGearMap().values().stream()
                .filter(slot -> slotIsValid(roles, slot))
                .collect(Collectors.toList());
    }

    private boolean slotIsValid(List<Role> roles, FisheryMemberGearBean slot) {
        if (slot.getGear() == FisheryGear.ROLE) {
            return slot.getLevel() < roles.size() &&
                    BotPermissionUtil.can(getGuild().get(), Permission.MANAGE_ROLES) &&
                    getGuild().get().getSelfMember().canInteract(roles.get(slot.getLevel()));
        }

        if (slot.getGear() == FisheryGear.TREASURE) {
            return guildBean.isFisheryTreasureChests();
        }

        return true;
    }

    private long calculateRolePrice(FisheryMemberGearBean slot) {
        return Fishery.getFisheryRolePrice(getGuild().get(), fisheryGuildBean.getRoles().size(), slot.getLevel());
    }

}
