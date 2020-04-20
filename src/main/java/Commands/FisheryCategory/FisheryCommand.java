package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.*;
import Core.*;
import Core.BotResources.ResourceManager;
import Core.Mention.MentionTools;
import Core.Tools.StringTools;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryServerBean;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "fishery",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\u2699\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/thegirltyler/brand-camp/128/Fishing-Worm-icon.png",
        executable = true,
        aliases = {"fishingsetup", "fisherysetup"}
)
public class FisheryCommand extends Command implements OnNavigationListener, OnReactionAddStaticListener {

    private static final int MAX_ROLES = 50;
    final static Logger LOGGER = LoggerFactory.getLogger(FisheryCommand.class);

    private ServerBean serverBean;
    private FisheryServerBean fisheryServerBean;
    private boolean stopLock = true;
    private CustomObservableList<Role> roles;
    private CustomObservableList<ServerTextChannel> ignoredChannels;

    public static final String treasureEmoji = "\uD83D\uDCB0";
    public static final String keyEmoji = "\uD83D\uDD11";
    private static final ArrayList<Message> blockedTreasureMessages = new ArrayList<>();

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
        fisheryServerBean = DBFishery.getInstance().getBean(event.getServer().get().getId());
        roles = fisheryServerBean.getRoles();
        ignoredChannels = fisheryServerBean.getIgnoredChannelIds().transform(channelId -> event.getServer().get().getTextChannelById(channelId), channel -> channel.getId());

        checkRolesWithLog(roles, null);
        return true;
    }
    
    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        switch (state) {
            case 1:
                ArrayList<Role> roleList = MentionTools.getRoles(event.getMessage(), inputString).getList();
                if (roleList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    if (!checkRolesWithLog(roleList, event.getMessage().getUserAuthor().get())) return Response.FALSE;

                    int existingRoles = 0;
                    for (Role role : roleList) {
                        if (fisheryServerBean.getRoleIds().contains(role.getId())) existingRoles++;
                    }

                    if (existingRoles >= roleList.size()) {
                        setLog(LogStatus.FAILURE, getString("roleexists", roleList.size() != 1));
                        return Response.FALSE;
                    }

                    int rolesAdded = 0;
                    for (Role role : roleList) {
                        if (!roles.contains(role)) {
                            roles.add(role);
                            rolesAdded++;
                            roles.sort(Comparator.comparingInt(Role::getPosition));
                        }
                    }

                    setLog(LogStatus.SUCCESS, getString("roleadd", (rolesAdded - existingRoles) != 1, String.valueOf(rolesAdded)));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
                ArrayList<ServerTextChannel> channelIgnoredList = MentionTools.getTextChannels(event.getMessage(), inputString).getList();
                if (channelIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    fisheryServerBean.getIgnoredChannelIds().clear();
                    fisheryServerBean.getIgnoredChannelIds().addAll(channelIgnoredList.stream().map(DiscordEntity::getId).collect(Collectors.toList()));
                    setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 4:
                ArrayList<ServerTextChannel> channelList = MentionTools.getTextChannels(event.getMessage(), inputString).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    ServerTextChannel channel = channelList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        serverBean.setFisheryAnnouncementChannelId(channel.getId());
                        setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 5:
                if (inputString.contains("-") && !inputString.replaceFirst("-", "").contains("-")) {
                    String[] parts = (inputString + " ").split("-");
                    long priceMin = StringTools.filterNumberFromString(parts[0]);
                    long priceMax = StringTools.filterNumberFromString(parts[1]);

                    if (priceMin >= -1 && priceMax >= -1) {
                        if (priceMin == -1) priceMin = serverBean.getFisheryRoleMin();
                        if (priceMax == -1) priceMax = serverBean.getFisheryRoleMax();
                        serverBean.setFisheryRolePrices(priceMin, priceMax);
                        setLog(LogStatus.SUCCESS, getString("pricesset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("prices_notnegative"));
                        return Response.FALSE;
                    }
                } else {
                    setLog(LogStatus.FAILURE, getString("prices_wrongvalues"));
                    return Response.FALSE;
                }
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        switch (state) {
            case 0:
                switch (i) {
                    case -1:
                        deleteNavigationMessage();
                        return false;

                    case 0:
                        serverBean.toggleFisheryTreasureChests();
                        setLog(LogStatus.SUCCESS, getString("treasurechestsset", serverBean.isFisheryTreasureChests()));
                        return true;

                    case 1:
                        serverBean.toggleFisheryReminders();
                        setLog(LogStatus.SUCCESS, getString("remindersset", serverBean.isFisheryReminders()));
                        return true;

                    case 2:
                        if (fisheryServerBean.getRoleIds().size() < MAX_ROLES) {
                            setState(1);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanyroles", String.valueOf(MAX_ROLES)));
                            return true;
                        }

                    case 3:
                        if (fisheryServerBean.getRoleIds().size() > 0) {
                            setState(2);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("norolesset"));
                            return true;
                        }

                    case 4:
                        setState(3);
                        return true;

                    case 5:
                        serverBean.toggleFisherySingleRoles();
                        setLog(LogStatus.SUCCESS, getString("singleroleset", serverBean.isFisherySingleRoles()));
                        return true;

                    case 6:
                        setState(4);
                        return true;

                    case 7:
                        setState(5);
                        return true;

                    case 8:
                        if (serverBean.getFisheryStatus() != FisheryStatus.ACTIVE) {
                            serverBean.setFisheryStatus(FisheryStatus.ACTIVE);
                            stopLock = true;
                        } else {
                            serverBean.setFisheryStatus(FisheryStatus.PAUSED);
                        }
                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        return true;

                    case 9:
                        if (serverBean.getFisheryStatus() == FisheryStatus.ACTIVE) {
                            if (stopLock) {
                                stopLock = false;
                                setLog(LogStatus.WARNING, getString("stoplock"));
                                return true;
                            } else {
                                DBFishery.getInstance().removePowerPlant(event.getServer().get().getId());
                                setLog(LogStatus.SUCCESS, getString("setstatus"));
                                return true;
                            }
                        }
                }
                break;

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                break;

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < fisheryServerBean.getRoleIds().size()) {
                    roles.remove(i);
                    setLog(LogStatus.SUCCESS, getString("roleremove"));
                    if (fisheryServerBean.getRoleIds().size() == 0) setState(0);
                    return true;
                }
                break;

            case 3:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        fisheryServerBean.getIgnoredChannelIds().clear();
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                        return true;
                }
                break;

            case 4:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        serverBean.setFisheryAnnouncementChannelId(null);
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                        return true;
                }
                break;

            case 5:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                break;
        }
        return false;
    }

    private String getRoleString(Role role) {
        int n = roles.indexOf(role);
        try {
            return getString("state0_rolestring", role.getMentionTag(), StringTools.numToString(getFisheryRolePrice(role.getServer(), new ArrayList<>(fisheryServerBean.getRoleIds()), n)));
        } catch (ExecutionException e) {
            LOGGER.error("Exception", e);
            return "";
        }
    }

    public static long getFisheryRolePrice(Server server, List<Long> roleIds, int n) throws ExecutionException {
        ServerBean serverBean = DBServer.getInstance().getBean(server.getId());

        double priceIdealMin = serverBean.getFisheryRoleMin();
        double priceIdealMax = serverBean.getFisheryRoleMax();

        if (roleIds.size() == 1) return (long) priceIdealMin;

        double power = Math.pow(priceIdealMax / priceIdealMin, 1 / (double)(roleIds.size() - 1));

        double price = Math.pow(power, n);
        double priceMax = Math.pow(power, roleIds.size() - 1);

        return Math.round(price * (priceIdealMax / priceMax));
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setOptions(getString("state0_options_"+ serverBean.getFisheryStatus().ordinal()).split("\n"));

                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description", String.valueOf(MAX_ROLES)))
                        .addField(getString("state0_mstatus"), "**" + getString("state0_status").split("\n")[serverBean.getFisheryStatus().ordinal()].toUpperCase() + "**", true)
                        .addField(getString("state0_mtreasurechests"), StringTools.getOnOffForBoolean(getLocale(), serverBean.isFisheryTreasureChests()), true)
                        .addField(getString("state0_mreminders"), StringTools.getOnOffForBoolean(getLocale(), serverBean.isFisheryReminders()), true)
                        .addField(getString("state0_mroles"), new ListGen<Role>().getList(roles, getLocale(), this::getRoleString), false)
                        .addField(getString("state0_mchannels"), new ListGen<ServerTextChannel>().getList(ignoredChannels, getLocale(), Mentionable::getMentionTag), false)
                        .addField(getString("state0_mannouncementchannel"), serverBean.getFisheryAnnouncementChannel().map(Mentionable::getMentionTag).orElse(notSet), false)
                        .addField(getString("state0_msinglerole", StringTools.getOnOffForBoolean(getLocale(), serverBean.isFisherySingleRoles())), getString("state0_msinglerole_desc"), false)
                        .addField(getString("state0_mroleprices"), getString("state0_mroleprices_desc", StringTools.numToString(getLocale(), serverBean.getFisheryRoleMin()), StringTools.numToString(getLocale(), serverBean.getFisheryRoleMax())), false);

            case 1:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                String[] roleStrings = new String[roles.size()];
                for(int i = 0; i < roleStrings.length; i++) {
                    roleStrings[i] = getRoleString(roles.get(i));
                }
                setOptions(roleStrings);

                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("state2_description"), getString("state2_title"));
                return eb;

            case 3:
                setOptions(new String[]{getString("state3_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state3_description"), getString("state3_title"));

            case 4:
                setOptions(new String[]{getString("state4_options")});
                return EmbedFactory.getCommandEmbedStandard(this, getString("state4_description"), getString("state4_title"));

            case 5:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state5_description"), getString("state5_title"));
        }
        return null;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    @Override
    public void onReactionAddStatic(Message message, ReactionAddEvent event) throws Throwable {
        if (event.getEmoji().getMentionTag().equalsIgnoreCase(keyEmoji)) {
            boolean blocked = false;
            for(Message message1: blockedTreasureMessages) {
                if (message1.getId() == message.getId()) {
                    blocked = true;
                    break;
                }
            }

            if (!blocked) {
                blockedTreasureMessages.add(message);
                if (message.getChannel().canYouRemoveReactionsOfOthers()) message.removeAllReactions().get();

                EmbedBuilder eb = EmbedFactory.getEmbed()
                        .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(getLocale(), TextManager.COMMANDS, "fishery_treasure_title"))
                        .setDescription(TextManager.getString(getLocale(), TextManager.COMMANDS, "fishery_treasure_opening", event.getUser().getMentionTag()));
                message.edit(eb).get();

                Thread.sleep(1000 * 3);

                Random r = new Random();
                String[] winLose = new String[]{"win", "lose"};
                int resultInt = r.nextInt(2);
                String result = winLose[resultInt];

                FisheryUserBean userBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(event.getUser().getId());
                long won = Math.round(userBean.getPowerUp(FisheryCategoryInterface.PER_TREASURE).getEffect() * (0.7 + r.nextDouble() * 0.6));

                eb = EmbedFactory.getEmbed()
                        .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(getLocale(), TextManager.COMMANDS, "fishery_treasure_title"))
                        .setDescription(TextManager.getString(getLocale(), TextManager.COMMANDS, "fishery_treasure_opened_" + result, event.getUser().getMentionTag(), StringTools.numToString(getLocale(), won)))
                        .setImage(ResourceManager.getFile(ResourceManager.RESOURCES, "treasure_opened_" + result + ".png"))
                        .setFooter(getString("treasure_footer"));
                message.edit(eb);
                if (message.getChannel().canYouRemoveReactionsOfOthers()) message.removeAllReactions();

                ServerTextChannel channel = event.getServerTextChannel().get();
                if (resultInt == 0 && channel.canYouWrite() && channel.canYouEmbedLinks()) channel.sendMessage(userBean.changeValues(0, won)).get();

                Thread t = new CustomThread(() -> {
                    try {
                        Thread.sleep(1000 * 60);
                        blockedTreasureMessages.remove(message);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                    }
                }, "treasure_block_countdown", 1);
                t.start();
            }
        } else {
            event.removeReaction();
        }
    }

    @Override
    public String getTitleStartIndicator() {
        return treasureEmoji;
    }
}
