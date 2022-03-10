package commands.runnables.fisherycategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import modules.fishery.FisheryGear;
import constants.LogStatus;
import commands.listeners.MessageInputResponse;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import modules.fishery.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.fisheryusers.FisheryMemberGearData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@CommandProperties(
        trigger = "buy",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "📥",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "shop", "upgrade", "invest", "levelup", "b" }
)
public class BuyCommand extends NavigationAbstract implements FisheryInterface {

    private FisheryMemberData fisheryMemberBean;
    private FisheryGuildData fisheryGuildBean;
    private GuildData guildBean;

    public BuyCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        fisheryMemberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberData(event.getMember().getIdLong());
        fisheryGuildBean = fisheryMemberBean.getFisheryGuildData();

        checkRolesWithLog(event.getGuild(), fisheryGuildBean.getRoles());
        if (args.length() > 0) {
            String letters = StringUtil.filterLettersFromString(args).toLowerCase().replace(" ", "");
            long numbers = StringUtil.filterLongFromString(args);
            FisheryGear fisheryGear = FisheryGear.parse(letters);

            long amount = 1;
            if (numbers != -1) {
                if (numbers >= 1 && numbers <= 100) {
                    amount = numbers;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "100"));
                    registerNavigationListener(event.getMember());
                    return true;
                }
            }

            if (fisheryGear != null) {
                for (int j = 0; j < amount; j++) {
                    if (!buy(fisheryGear, event.getMember(), false)) {
                        break;
                    }
                }

                registerNavigationListener(event.getMember());
                return true;
            }

            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
        }

        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String inputString, int state) throws Throwable {
        return null;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                deregisterListenersWithComponentMessage();
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

        boolean canUseTreasureChests = slotIsValid(member.getGuild(), roles, fisheryMemberBean.getMemberGear(FisheryGear.TREASURE));
        boolean canUseRoles = slotIsValid(member.getGuild(), roles, fisheryMemberBean.getMemberGear(FisheryGear.ROLE));

        if (transferableSlots) {
            if (i >= FisheryGear.TREASURE.ordinal() && !canUseTreasureChests) i++;
            if (i >= FisheryGear.ROLE.ordinal() && !canUseRoles) i++;
        } else {
            if (i == FisheryGear.TREASURE.ordinal() && !canUseTreasureChests) return false;
            if (i == FisheryGear.ROLE.ordinal() && !canUseRoles) return false;
        }
        if (i > FisheryGear.values().length - 1 || i < 0) return false;
        fisheryGear = FisheryGear.values()[i];

        FisheryMemberGearData slot = fisheryMemberBean.getMemberGear(fisheryGear);

        long price = slot.getPrice();
        if (slot.getGear() == FisheryGear.ROLE) {
            price = calculateRolePrice(member.getGuild(), slot);
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

    private void upgrade(FisheryMemberGearData slot, long price, List<Role> roles, Member member) {
        fisheryMemberBean.changeValues(0, -price);
        fisheryMemberBean.levelUp(slot.getGear());

        if (slot.getGear() == FisheryGear.ROLE) {
            Fishery.synchronizeRoles(member);
            Optional<TextChannel> announcementChannelOpt = guildBean.getFisheryAnnouncementChannel();
            if (announcementChannelOpt.isPresent() && PermissionCheckRuntime.botHasPermission(getLocale(), getClass(), announcementChannelOpt.get(), Permission.MESSAGE_SEND)) {
                String announcementText = getString("newrole", member.getUser().getAsMention(), StringUtil.escapeMarkdown(roles.get(slot.getLevel() - 1).getName()), String.valueOf(slot.getLevel()));
                announcementChannelOpt.get().sendMessage(announcementText).queue();
            }
        }
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        List<Role> roles = fisheryGuildBean.getRoles();

        switch (state) {
            case 0:
                ArrayList<String> options = new ArrayList<>();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("beginning") + "\n" + Emojis.ZERO_WIDTH_SPACE);
                int i = 0;
                for (FisheryMemberGearData slot : getUpgradableGears()) {
                    String productDescription = "???";
                    long price = slot.getPrice();
                    if (slot.getGear() != FisheryGear.ROLE) {
                        productDescription = getString("product_des_" + slot.getGear().ordinal(), StringUtil.numToString(slot.getDeltaEffect()));
                    } else if (roles.get(slot.getLevel()) != null) {
                        price = calculateRolePrice(member.getGuild(), slot);
                        productDescription = getString("product_des_" + slot.getGear().ordinal(), roles.get(slot.getLevel()).getAsMention());
                    }

                    String title = getString("product_" + slot.getGear().ordinal() + "_0");
                    options.add(title);
                    eb.addField(
                            getString("product_title", slot.getGear().getEmoji(), title, StringUtil.numToString(slot.getLevel()), StringUtil.numToString(price)),
                            productDescription + "\n" + Emojis.ZERO_WIDTH_SPACE,
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
                        fisheryMemberBean.getGuildData().hasFisheryCoinsGivenLimit() ? StringUtil.numToString(fisheryMemberBean.getCoinsGiveReceivedMax()) : "∞"
                );

                eb.addField(getString("status_title"), StringUtil.shortenStringLine(status, 1024), false);
                setComponents(options.toArray(new String[0]));
                return eb;

            case 1:
                return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("{PREFIX}", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title"));

            default:
                return null;
        }
    }

    private List<FisheryMemberGearData> getUpgradableGears() {
        List<Role> roles = fisheryGuildBean.getRoles();
        Guild guild = roles.stream()
                .findFirst()
                .map(Role::getGuild)
                .orElse(null);

        return fisheryMemberBean.getGearMap().values().stream()
                .filter(slot -> slotIsValid(guild, roles, slot))
                .collect(Collectors.toList());
    }

    private boolean slotIsValid(Guild guild, List<Role> roles, FisheryMemberGearData slot) {
        if (slot.getGear() == FisheryGear.ROLE) {
            return slot.getLevel() < roles.size() &&
                    BotPermissionUtil.can(guild, Permission.MANAGE_ROLES) &&
                    BotPermissionUtil.canManage(roles.get(slot.getLevel()));
        }

        if (slot.getGear() == FisheryGear.TREASURE) {
            return guildBean.isFisheryTreasureChests();
        }

        return true;
    }

    private long calculateRolePrice(Guild guild, FisheryMemberGearData slot) {
        return Fishery.getFisheryRolePrice(guild, fisheryGuildBean.getRoles().size(), slot.getLevel());
    }

}
