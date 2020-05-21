package Commands.FisheryCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnNavigationListener;
import CommandSupporters.Command;
import Constants.*;
import Core.*;
import Core.Mention.MentionList;
import Core.Mention.MentionUtil;
import Core.Utils.StringUtil;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.List;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "fisherymanage",
        botPermissions = Permission.USE_EXTERNAL_EMOJIS,
        userPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83D\uDCB0",
        executable = false,
        patreonRequired = true,
        aliases = {"fishingmanage", "fishmanage", "fisheryusermanage", "fisherymanager"}
)
public class FisheryManageCommand extends Command implements OnNavigationListener {

    private User user;
    private Server server;
    private FisheryUserBean fisheryUserBean;
    private enum ValueProcedure { ABSOLUTE, ADD, SUB }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
        FisheryStatus status = serverBean.getFisheryStatus();
        if (status != FisheryStatus.ACTIVE) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }

        MentionList<User> userMentions = MentionUtil.getUsers(event.getMessage(), followedString);
        List<User> list = userMentions
                .getList()
                .stream()
                .filter(user -> !user.isBot())
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions_no_bots")));
            return false;
        }

        server = event.getServer().get();
        user = list.get(0);
        fisheryUserBean = DBFishery.getInstance().getBean(event.getServer().get().getId()).getUserBean(user.getId());

        followedString = userMentions.getResultMessageString();
        if (followedString.length() > 0) {
            String typeString = followedString.split(" ")[0];

            int type = -1;
            switch (typeString.toLowerCase()) {
                case "fish": type = 0; break;
                case "coins": case "coin": type = 1; break;
                case "daily": case "dailystreak": type = 2; break;
            }

            if (type == -1) {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
            } else {
                String amountString = StringUtil.trimString(followedString.substring(typeString.length()));
                Long value;
                if ((value = updateValues(type, amountString)) != null) {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("set", type, user.getMentionTag(), StringUtil.numToString(getLocale(), value))));
                    removeNavigation();
                    return true;
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")));
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable {
        if (state >= 1) {
            Long value;
            if ((value = updateValues(state - 1, inputString)) == null) {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                return Response.FALSE;
            }

            setLog(LogStatus.SUCCESS, getString("set_log", state - 1, user.getDisplayName(server), StringUtil.numToString(getLocale(), value)).replace("*", ""));
            setState(0);

            return Response.TRUE;
        }

        return null;
    }

    private Long updateValues(int type, String inputString) {
        ValueProcedure valueProcedure = ValueProcedure.ABSOLUTE;
        if (inputString.startsWith("+")) {
            valueProcedure = ValueProcedure.ADD;
            inputString = inputString.substring(1);
        } else if (inputString.startsWith("-")) {
            valueProcedure = ValueProcedure.SUB;
            inputString = inputString.substring(1);
        }

        if (inputString.length() == 0 || !Character.isDigit(inputString.charAt(0))) return null;

        long baseValue;
        switch (type) {
            case 0: baseValue = fisheryUserBean.getFish(); break;
            case 1: baseValue = fisheryUserBean.getCoins(); break;
            case 2: baseValue = fisheryUserBean.getDailyStreak(); break;
            default: baseValue = 0;
        }

        long newValue = MentionUtil.getAmountExt(inputString, baseValue);
        if (newValue == -1) {
            return null;
        }

        switch (valueProcedure) {
            case ADD: newValue = baseValue + newValue; break;
            case SUB: newValue = baseValue - newValue; break;
        }
        if (newValue < 0) newValue = 0;
        if (newValue > Settings.MAX) newValue = Settings.MAX;

        switch (type) {
            /* Fish */
            case 0: fisheryUserBean.setFish(newValue); break;

            /* Coins */
            case 1: fisheryUserBean.setCoins(newValue); break;

            /* Daily Streak */
            case 2: fisheryUserBean.setDailyStreak(newValue); break;
        }

        return newValue;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                removeNavigationWithMessage();
                return false;
            } else if (i >= 0 && i <= 2) {
                setState(i + 1);
                return true;
            }
        } else if (i == -1) {
            setState(0);
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        if (state == 0) {
            setOptions(
                    getString("state0_options",
                        StringUtil.numToString(getLocale(), fisheryUserBean.getFish()),
                        StringUtil.numToString(getLocale(), fisheryUserBean.getCoins()),
                        StringUtil.numToString(getLocale(), fisheryUserBean.getDailyStreak())
                    ).split("\n")
            );

            String desc = getString("state0_description", user.getMentionTag());
            return EmbedFactory.getCommandEmbedStandard(this, desc);
        } else {
            return EmbedFactory.getCommandEmbedStandard(this,
                    getString("state1_description",
                            state - 1,
                            StringUtil.numToString(getLocale(), fisheryUserBean.getFish()),
                            StringUtil.numToString(getLocale(), fisheryUserBean.getCoins()),
                            StringUtil.numToString(getLocale(), fisheryUserBean.getDailyStreak())),
                    getString("state1_title", state - 1)
            );
        }
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 3;
    }

}
