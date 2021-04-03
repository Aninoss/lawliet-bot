package commands.runnables.fisherysettingscategory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Response;
import constants.Settings;
import core.EmbedFactory;
import core.TextManager;
import core.mention.MentionList;
import core.utils.MentionUtil;
import modules.FisheryMemberGroup;
import mysql.modules.fisheryusers.FisheryMemberBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "fisherymanage",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "💰",
        executableWithoutArgs = false,
        patreonRequired = true,
        aliases = { "fishingmanage", "fishmanage", "fisheryusermanage", "fisherymanager" }
)
public class FisheryManageCommand extends NavigationAbstract implements FisheryInterface {

    private enum ValueProcedure { ABSOLUTE, ADD, SUB }

    private FisheryMemberGroup fisheryMemberGroup;
    private boolean resetLog = true;

    public FisheryManageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) {
        MentionList<Member> userMentions = MentionUtil.getMembers(event.getMessage(), args);
        ArrayList<Member> list = userMentions
                .getList()
                .stream()
                .filter(member -> !member.getUser().isBot())
                .collect(Collectors.toCollection(ArrayList::new));

        MentionList<Role> roleMentions = MentionUtil.getRoles(event.getMessage(), userMentions.getFilteredArgs());
        roleMentions.getList().forEach(role -> {
            event.getGuild().getMembersWithRoles(role).stream()
                    .filter(member -> !member.getUser().isBot() && !list.contains(member))
                    .forEach(list::add);
        });

        if (list.isEmpty()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions_no_bots")).build())
                    .queue();
            return false;
        }

        fisheryMemberGroup = new FisheryMemberGroup(event.getGuild().getIdLong(), list);

        args = roleMentions.getFilteredArgs();
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

                case "reset":
                case "remove":
                case "delete":
                case "clear":
                    type = 3;
                    break;

                default:
            }

            if (type == -1) {
                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
            } else if (type == 3) {
                fisheryMemberGroup.getFisheryMemberList().forEach(FisheryMemberBean::remove);
                event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("reset", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getAsTag())).build())
                        .queue();
                return true;
            } else {
                String amountString = args.substring(typeString.length()).trim();
                if (updateValues(type, amountString)) {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set", type, fisheryMemberGroup.getAsTag(), amountString)).build())
                            .queue();
                    return true;
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")).build())
                            .queue();
                    return false;
                }
            }
        }

        registerNavigationListener(4);
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) {
        if (state >= 1) {
            if (!updateValues(state - 1, input)) {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                return Response.FALSE;
            }

            setLog(LogStatus.SUCCESS, getString("set_log", state - 1, fisheryMemberGroup.getAsTag(), input).replace("*", ""));
            resetLog = true;
            setState(0);

            return Response.TRUE;
        }

        return null;
    }

    private boolean updateValues(int type, String inputString) {
        boolean success = false;
        ValueProcedure valueProcedure = ValueProcedure.ABSOLUTE;
        if (inputString.startsWith("+")) {
            valueProcedure = ValueProcedure.ADD;
            inputString = inputString.substring(1);
        } else if (inputString.startsWith("-")) {
            valueProcedure = ValueProcedure.SUB;
            inputString = inputString.substring(1);
        }

        if (inputString.length() == 0 || !Character.isDigit(inputString.charAt(0))) {
            return false;
        }

        for (FisheryMemberBean fisheryMemberBean : fisheryMemberGroup.getFisheryMemberList()) {
            long baseValue = getBaseValueByType(fisheryMemberBean, type);
            long newValue = MentionUtil.getAmountExt(inputString, baseValue);
            if (newValue == -1) {
                continue;
            }

            newValue = calculateNewValue(baseValue, newValue, valueProcedure);
            setNewValues(fisheryMemberBean, newValue, type);
            success = true;
        }

        return success;
    }

    private void setNewValues(FisheryMemberBean fisheryMemberBean, long newValue, int type) {
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
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            if (i == -1) {
                removeNavigationWithMessage();
                return false;
            } else if (i >= 0 && i <= 2) {
                setState(i + 1);
                return true;
            } else if (i == 3) {
                if (resetLog) {
                    resetLog = false;
                    setLog(LogStatus.WARNING, getString("state0_confirm"));
                } else {
                    fisheryMemberGroup.getFisheryMemberList().forEach(FisheryMemberBean::remove);
                    resetLog = true;
                    setLog(LogStatus.SUCCESS, getString("reset_log", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getAsTag()));
                    setState(0);
                }
                return true;
            }
        } else if (i == -1) {
            resetLog = true;
            setState(0);
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(int state) throws Throwable {
        String[] values = new String[] {
                fisheryMemberGroup.getFishString(),
                fisheryMemberGroup.getCoinsString(),
                fisheryMemberGroup.getDailyStreakString()
        };

        if (state == 0) {
            setOptions(getString("state0_options", values).split("\n"));

            String desc = getString("state0_description", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getAsTag());
            return EmbedFactory.getEmbedDefault(this, desc);
        } else {
            return EmbedFactory.getEmbedDefault(
                    this,
                    getString("state1_description", state - 1, values),
                    getString("state1_title", state - 1)
            );
        }
    }

}
