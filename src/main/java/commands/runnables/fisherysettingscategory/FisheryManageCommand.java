package commands.runnables.fisherysettingscategory;

import commands.listeners.CommandProperties;

import commands.Command;
import constants.*;
import core.*;
import core.mention.MentionList;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberBean;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildBean;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "fisherymanage",
        botPermissions = PermissionDeprecated.USE_EXTERNAL_EMOJIS,
        userPermissions = PermissionDeprecated.MANAGE_SERVER,
        emoji = "💰",
        executableWithoutArgs = false,
        patreonRequired = true,
        aliases = {"fishingmanage", "fishmanage", "fisheryusermanage", "fisherymanager"}
)
public class FisheryManageCommand extends Command implements OnNavigationListenerOld {

    private long userId;
    private Server server;
    private FisheryMemberBean fisheryMemberBean;

    public FisheryManageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    private enum ValueProcedure { ABSOLUTE, ADD, SUB }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String args) throws Throwable {
        GuildBean guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        FisheryStatus status = guildBean.getFisheryStatus();
        if (status != FisheryStatus.ACTIVE) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("%PREFIX", getPrefix()), TextManager.getString(getLocale(), TextManager.GENERAL, "fishing_notactive_title")));
            return false;
        }

        MentionList<User> userMentions = MentionUtil.getMembers(event.getMessage(), args);
        List<User> list = userMentions
                .getList()
                .stream()
                .filter(user -> !user.isBot())
                .collect(Collectors.toList());

        if (list.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions_no_bots")));
            return false;
        }

        server = event.getServer().get();
        userId = list.get(0).getId();
        fisheryMemberBean = DBFishery.getInstance().retrieve(event.getGuild().getIdLong()).getMemberBean(userId);

        args = userMentions.getResultMessageString();
        if (args.length() > 0) {
            String typeString = args.split(" ")[0];

            int type = -1;
            switch (typeString.toLowerCase()) {
                case "fish":
                    type = 0;
                    break;

                case "coins":
                    case "coin":
                        type = 1;
                        break;

                case "daily":
                    case "dailystreak":
                        case "streak":
                            type = 2;
                            break;

                default:
            }

            if (type == -1) {
                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
            } else {
                String amountString = args.substring(typeString.length()).trim();
                Long value;
                AtomicLong valueOld = new AtomicLong();
                if ((value = updateValues(type, amountString, valueOld)) != null) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set", type, MentionUtil.getUserMentionTag(userId), StringUtil.numToString(valueOld.get()), StringUtil.numToString(value))));
                    removeNavigation();
                    return true;
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")));
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
            AtomicLong valueOld = new AtomicLong();
            if ((value = updateValues(state - 1, inputString, valueOld)) == null) {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                return Response.FALSE;
            }

            setLog(LogStatus.SUCCESS, getString("set_log", state - 1, server.getMemberById(userId).map(u -> server.getDisplayName(u)).orElse(MentionUtil.getUserMentionTag(userId)), StringUtil.numToString(valueOld.get()), StringUtil.numToString(value)).replace("*", ""));
            setState(0);

            return Response.TRUE;
        }

        return null;
    }

    private Long updateValues(int type, String inputString, AtomicLong valueOld) {
        ValueProcedure valueProcedure = ValueProcedure.ABSOLUTE;
        if (inputString.startsWith("+")) {
            valueProcedure = ValueProcedure.ADD;
            inputString = inputString.substring(1);
        } else if (inputString.startsWith("-")) {
            valueProcedure = ValueProcedure.SUB;
            inputString = inputString.substring(1);
        }

        if (inputString.length() == 0 || !Character.isDigit(inputString.charAt(0))) return null;

        long baseValue = getBaseValueByType(fisheryMemberBean, type);
        valueOld.set(baseValue);
        long newValue = MentionUtil.getAmountExt(inputString, baseValue);
        if (newValue == -1)
            return null;

        newValue = calculateNewValue(baseValue, newValue, valueProcedure);
        setNewValues(newValue, type);

        return newValue;
    }

    private void setNewValues(long newValue, int type) {
        switch (type) {
            /* Fish */
            case 0:
                fisheryMemberBean.setFish(newValue);
                break;

            /* Coins */
            case 1:
                fisheryMemberBean.setCoinsRaw(newValue + fisheryMemberBean.getCoinsHidden());
                break;

            /* Daily Streak */
            case 2:
                fisheryMemberBean.setDailyStreak(newValue);
                break;

            default:
        }
    }

    private long calculateNewValue(long baseValue, long newValue, ValueProcedure valueProcedure) {
        switch (valueProcedure) {
            case ADD:
                newValue = baseValue + newValue;
                break;

            case SUB:
                newValue = baseValue - newValue;
                break;

            default:
        }
        if (newValue < 0) newValue = 0;
        if (newValue > Settings.FISHERY_MAX) newValue = Settings.FISHERY_MAX;

        return newValue;
    }

    private long getBaseValueByType(FisheryMemberBean fisheryMemberBean, int type) {
        switch (type) {
            case 0:
                return fisheryMemberBean.getFish();

            case 1:
                return fisheryMemberBean.getCoins();

            case 2:
                return fisheryMemberBean.getDailyStreak();

            default:
                throw new IndexOutOfBoundsException("invalid type");
        }
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
                        StringUtil.numToString(fisheryMemberBean.getFish()),
                        StringUtil.numToString(fisheryMemberBean.getCoins()),
                        StringUtil.numToString(fisheryMemberBean.getDailyStreak())
                    ).split("\n")
            );

            String desc = getString("state0_description", MentionUtil.getUserMentionTag(userId));
            return EmbedFactory.getEmbedDefault(this, desc);
        } else {
            return EmbedFactory.getEmbedDefault(this,
                    getString("state1_description",
                            state - 1,
                            StringUtil.numToString(fisheryMemberBean.getFish()),
                            StringUtil.numToString(fisheryMemberBean.getCoins()),
                            StringUtil.numToString(fisheryMemberBean.getDailyStreak())),
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
