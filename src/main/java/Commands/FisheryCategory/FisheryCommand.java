package Commands.FisheryCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Constants.*;
import General.*;
import General.BotResources.ResourceManager;
import General.Mention.MentionFinder;
import MySQL.DBServer;
import MySQL.DBUser;
import MySQL.DatabaseCache;
import MySQL.FisheryCache;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.Mentionable;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "fishery",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS_IN_TEXT_CHANNEL,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\u2699\uFE0F️",
        thumbnail = "http://icons.iconarchive.com/icons/thegirltyler/brand-camp/128/Fishing-Worm-icon.png",
        executable = true,
        aliases = {"fishingsetup", "fisherysetup"}
)
public class FisheryCommand extends Command implements onNavigationListener,onReactionAddStatic  {

    private static final int MAX_ROLES = 50;
    private int page, pageMax;

    private ArrayList<Role> roles;
    private ArrayList<ServerTextChannel> ignoredChannels;
    private PowerPlantStatus status;
    private boolean singleRole, treasureChests, reminders;
    private ServerTextChannel announcementChannel;
    private Pair<Long, Long> rolePrices;

    public static final String treasureEmoji = "\uD83D\uDCB0";
    public static final String keyEmoji = "\uD83D\uDD11";
    private static ArrayList<Message> blockedTreasureMessages = new ArrayList<>();
    private static ArrayList<Server> busyServers = new ArrayList<>();
    
    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            roles = DBServer.getPowerPlantRolesFromServer(event.getServer().get());
            status = DBServer.getPowerPlantStatusFromServer(event.getServer().get());
            ignoredChannels = DBServer.getPowerPlantIgnoredChannelsFromServer(event.getServer().get());
            singleRole = DBServer.getPowerPlantSingleRoleFromServer(event.getServer().get());
            announcementChannel = DBServer.getPowerPlantAnnouncementChannelFromServer(event.getServer().get());
            treasureChests = DBServer.getPowerPlantTreasureChestsFromServer(event.getServer().get());
            reminders = DBServer.getPowerPlantRemindersFromServer(event.getServer().get());
            rolePrices = DBServer.getFisheryRolePrices(event.getServer().get());

            checkRolesWithLog(roles, null);

            return Response.TRUE;
        }

        switch (state) {
            case 1:
                ArrayList<Role> roleList = MentionFinder.getRoles(event.getMessage(), inputString).getList();
                if (roleList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    if (!checkRolesWithLog(roleList, event.getMessage().getUserAuthor().get())) return Response.FALSE;

                    int existingRoles = 0;
                    for (Role role : roleList) {
                        if (roles.contains(role)) existingRoles++;
                    }

                    if (existingRoles >= roleList.size()) {
                        setLog(LogStatus.FAILURE, getString("roleexists", roleList.size() != 1));
                        return Response.FALSE;
                    }

                    for (Role role : roleList) {
                        if (!roles.contains(role)) {
                            roles.add(role);
                            roles.sort(Comparator.comparingInt(Role::getPosition));
                            DBServer.addPowerPlantRoles(event.getServer().get(), role);
                        }
                    }

                    setLog(LogStatus.SUCCESS, getString("roleadd", (roleList.size() - existingRoles) != 1));
                    setState(0);
                    return Response.TRUE;
                }

            case 3:
                ArrayList<ServerTextChannel> channelIgnoredList = MentionFinder.getTextChannels(event.getMessage(), inputString).getList();
                if (channelIgnoredList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    ignoredChannels = channelIgnoredList;
                    DBServer.savePowerPlantIgnoredChannels(event.getServer().get(), ignoredChannels);
                    setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                    setState(0);
                    return Response.TRUE;
                }

            case 4:
                ArrayList<ServerTextChannel> channelList = MentionFinder.getTextChannels(event.getMessage(), inputString).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", inputString));
                    return Response.FALSE;
                } else {
                    ServerTextChannel channel = channelList.get(0);
                    if (checkWriteInChannelWithLog(channel)) {
                        announcementChannel = channel;
                        setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                        setState(0);
                        DBServer.savePowerPlantAnnouncementChannel(event.getServer().get(), announcementChannel);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 5:
                if (inputString.contains("-") && !inputString.replaceFirst("-", "").contains("-")) {
                    String[] parts = inputString.split("-");
                    long priceMin = Tools.filterNumberFromString(parts[0]);
                    long priceMax = Tools.filterNumberFromString(parts[1]);

                    if (priceMin >= -1 && priceMax >= -1) {
                        if (priceMin == -1) priceMin = rolePrices.getKey();
                        if (priceMax == -1) priceMax = rolePrices.getValue();
                        rolePrices = new Pair<>(priceMin, priceMax);
                        DBServer.savePowerPlantRolePrices(event.getServer().get(), rolePrices);
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
                        treasureChests = !treasureChests;
                        DBServer.savePowerPlantTreasureChestsSetting(event.getServer().get(), treasureChests);
                        setLog(LogStatus.SUCCESS, getString("treasurechestsset", treasureChests));
                        return true;

                    case 1:
                        reminders = !reminders;
                        DBServer.savePowerPlantRemindersSetting(event.getServer().get(), reminders);
                        setLog(LogStatus.SUCCESS, getString("remindersset", reminders));
                        return true;

                    case 2:
                        if (roles.size() < MAX_ROLES) {
                            setState(1);
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, getString("toomanyroles", String.valueOf(MAX_ROLES)));
                            return true;
                        }

                    case 3:
                        if (status == PowerPlantStatus.STOPPED) {
                            if (roles.size() > 0) {
                                page = 0;
                                pageMax = (roles.size() - 1) / 10;
                                setState(2);
                                return true;
                            } else {
                                setLog(LogStatus.FAILURE, getString("norolesset"));
                                return true;
                            }
                        } else {
                            setLog(LogStatus.FAILURE, getString("roleremove_pprunniung"));
                            return true;
                        }

                    case 4:
                        setState(3);
                        return true;

                    case 5:
                        Server server = event.getServer().get();

                        if (!busyServers.contains(server)) {
                            singleRole = !singleRole;
                            DBServer.savePowerPlantSingleRole(server, singleRole);

                            Thread t = new Thread(() -> {
                                busyServers.add(server);

                                if (singleRole) {
                                    for (int j = roles.size() - 1; j >= 1; j--) {
                                        Role roleHighest = roles.get(j);

                                        for (User user : new ArrayList<>(roleHighest.getUsers())) {
                                            for (int k = 0; k < j; k++) {
                                                try {
                                                    Role role = roles.get(k);
                                                    if (role != null) user.removeRole(role).get();
                                                } catch (InterruptedException | ExecutionException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    for (int j = 1; j < roles.size(); j++) {
                                        Role roleHighest = roles.get(j);

                                        for (User user : roleHighest.getUsers()) {
                                            for (int k = 0; k < j; k++) {
                                                try {
                                                    Role role = roles.get(k);
                                                    if (role != null) user.addRole(role).get();
                                                } catch (InterruptedException | ExecutionException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                }

                                busyServers.remove(server);
                            });
                            t.setName("fishery_role_updater");
                            t.start();

                            setLog(LogStatus.SUCCESS, getString("singleroleset", singleRole));
                            return true;
                        } else {
                            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "role_busy"));
                            return true;
                        }

                    case 6:
                        setState(4);
                        return true;

                    case 7:
                        setState(5);
                        return true;

                    case 8:
                        if (status != PowerPlantStatus.ACTIVE) {
                            status = PowerPlantStatus.ACTIVE;
                            DBServer.savePowerPlantStatusSetting(event.getServer().get(), status);
                        } else {
                            status = PowerPlantStatus.PAUSED;
                            DBServer.savePowerPlantStatusSetting(event.getServer().get(), status);
                        }
                        setLog(LogStatus.SUCCESS, getString("setstatus"));
                        return true;

                    case 9:
                        if (status == PowerPlantStatus.ACTIVE) {
                            server = event.getServer().get();

                            if (!busyServers.contains(server)) {
                                status = PowerPlantStatus.STOPPED;
                                DBServer.removePowerPlant(server);
                                FisheryCache.getInstance(DiscordApiCollection.getInstance().getResponsibleShard(server.getId())).stopServer(server);
                                DatabaseCache.getInstance().fishingProfileRemoveServer(server);
                                Thread t = new Thread(() -> {
                                    busyServers.add(server);

                                    for (User user : event.getServer().get().getMembers()) {
                                        for (Role role : roles) {
                                            if (role.getUsers().contains(user)) {
                                                try {
                                                    role.removeUser(user).get();
                                                } catch (InterruptedException | ExecutionException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }

                                    busyServers.remove(server);
                                });
                                t.setName("fishery_role_updater");
                                t.start();
                                setLog(LogStatus.SUCCESS, getString("setstatus"));
                                return true;
                            } else {
                                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "role_busy"));
                                return true;
                            }
                        }
                }
                return false;

            case 1:
                if (i == -1) {
                    setState(0);
                    return true;
                }

            case 2:
                if (i == -1) {
                    setState(0);
                    return true;
                } else if (i < Math.min(10, roles.size() - 10 * page)) {
                    DBServer.removePowerPlantRoles(event.getServer().get(), roles.remove(i + page * 10));
                    setLog(LogStatus.SUCCESS, getString("roleremove"));
                    if (roles.size() == 0) setState(0);
                    return true;
                } else if (pageMax > 0) {
                    if (i == 10) {
                        page--;
                        if (page < 0) page = pageMax;
                        return true;
                    } else if (i == 11) {
                        page++;
                        if (page > pageMax) page = 0;
                        return true;
                    }
                }

            case 3:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        ignoredChannels = new ArrayList<>();
                        DBServer.savePowerPlantIgnoredChannels(event.getServer().get(), ignoredChannels);
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("ignoredchannelsset"));
                        return true;
                }
                return false;

            case 4:
                switch (i) {
                    case -1:
                        setState(0);
                        return true;

                    case 0:
                        announcementChannel = null;
                        DBServer.savePowerPlantAnnouncementChannel(event.getServer().get(), announcementChannel);
                        setState(0);
                        setLog(LogStatus.SUCCESS, getString("announcementchannelset"));
                        return true;
                }
                return false;

            case 5:
                if (i == -1) {
                    setState(0);
                    return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        switch (state) {
            case 0:
                setOptions(getString("state0_options_"+ status.ordinal()).split("\n"));
                return EmbedFactory.getCommandEmbedStandard(this, getString("state0_description", String.valueOf(MAX_ROLES)))
                        .addField(getString("state0_mstatus"), "**" + getString("state0_status").split("\n")[status.ordinal()].toUpperCase() + "**", true)
                        .addField(getString("state0_mtreasurechests"), Tools.getOnOffForBoolean(getLocale(), treasureChests), true)
                        .addField(getString("state0_mreminders"), Tools.getOnOffForBoolean(getLocale(), reminders), true)
                        .addField(getString("state0_mroles"), new ListGen<Role>().getList(roles, getLocale(), Role::getMentionTag), false)
                        .addField(getString("state0_mchannels"), new ListGen<ServerTextChannel>().getList(ignoredChannels, getLocale(), Mentionable::getMentionTag), false)
                        .addField(getString("state0_mannouncementchannel"), Tools.getStringIfNotNull(announcementChannel, notSet), false)
                        .addField(getString("state0_msinglerole", Tools.getOnOffForBoolean(getLocale(), singleRole)), getString("state0_msinglerole_desc"), false)
                        .addField(getString("state0_mroleprices"), getString("state0_mroleprices_desc", Tools.numToString(getLocale(), rolePrices.getKey()), Tools.numToString(getLocale(), rolePrices.getValue())), false);

            case 1:
                return EmbedFactory.getCommandEmbedStandard(this, getString("state1_description"), getString("state1_title"));

            case 2:
                String[] roleStrings = new String[Math.min(10, roles.size()) + (pageMax > 0 ? 2 : 0)];
                Arrays.fill(roleStrings, "");

                for(int i=0; i < Math.min(10, roles.size() - page * 10); i++) {
                    roleStrings[i] = roles.get(i + page * 10).getMentionTag();
                }
                if (pageMax > 0) {
                    roleStrings[10] = getString("state2_previous");
                    roleStrings[11] = getString("state2_next");
                }
                setOptions(roleStrings);

                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("state2_description", pageMax > 0, String.valueOf(page), String.valueOf(pageMax)), getString("state2_title"));
                if (pageMax > 0) eb.setFooter(getString("state2_page", String.valueOf(page + 1), String.valueOf(pageMax + 1)));
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

                long won = Math.round(DBUser.getFishingProfile(event.getServer().get(), event.getUser()).getEffect(FishingCategoryInterface.PER_TREASURE) * (0.7 + r.nextDouble() * 0.6));

                setPrefix(DBServer.getPrefix(event.getServer().get()));
                eb = EmbedFactory.getEmbed()
                        .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(getLocale(), TextManager.COMMANDS, "fishery_treasure_title"))
                        .setDescription(TextManager.getString(getLocale(), TextManager.COMMANDS, "fishery_treasure_opened_" + result, event.getUser().getMentionTag(), Tools.numToString(getLocale(), won)))
                        .setImage(ResourceManager.getFile(ResourceManager.RESOURCES, "treasure_opened_" + result + ".png"))
                        .setFooter(getString("treasure_footer"));
                message.edit(eb);
                if (message.getChannel().canYouRemoveReactionsOfOthers()) message.removeAllReactions();

                ServerTextChannel channel = event.getServerTextChannel().get();
                if (resultInt == 0 && channel.canYouWrite() && channel.canYouEmbedLinks()) channel.sendMessage(DBUser.addFishingValues(getLocale(), event.getServer().get(), event.getUser(), 0L, won)).get();

                Thread t = new Thread(() -> {
                    try {
                        Thread.sleep(1000 * 60);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    blockedTreasureMessages.remove(message);
                });
                t.setName("treasure_block_countdown");
                t.start();
            }
        } else {
            event.removeReaction();
        }
    }

    @Override
    public boolean requiresLocale() {
        return true;
    }

    @Override
    public String getTitleStartIndicator() {
        return treasureEmoji;
    }
}
