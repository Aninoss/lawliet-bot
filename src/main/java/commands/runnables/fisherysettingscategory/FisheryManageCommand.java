package commands.runnables.fisherysettingscategory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.MentionList;
import core.utils.EmojiUtil;
import core.utils.MentionUtil;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryManage;
import modules.fishery.FisheryMemberGroup;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

@CommandProperties(
        trigger = "fisherymanage",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "💰",
        executableWithoutArgs = false,
        patreonRequired = true,
        usesExtEmotes = true,
        requiresFullMemberCache = true,
        aliases = { "fishingmanage", "fishmanage", "fisheryusermanage", "fisherymanager", "fm", "managefishery" }
)
public class FisheryManageCommand extends NavigationAbstract implements FisheryInterface {

    private FisheryMemberGroup fisheryMemberGroup;
    private boolean resetLog = true;

    public FisheryManageCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) {
        MentionList<Member> userMentions = MentionUtil.getMembers(event.getGuild(), args, event.getRepliedMember());
        ArrayList<Member> list = userMentions
                .getList()
                .stream()
                .filter(member -> !member.getUser().isBot())
                .collect(Collectors.toCollection(ArrayList::new));

        MentionList<Role> roleMentions = MentionUtil.getRoles(event.getGuild(), userMentions.getFilteredArgs());
        roleMentions.getList().forEach(role -> {
            event.getGuild().getMembersWithRoles(role).stream()
                    .filter(member -> !member.getUser().isBot() && !list.contains(member))
                    .forEach(list::add);
        });

        if (list.isEmpty()) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions_no_bots")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        fisheryMemberGroup = new FisheryMemberGroup(event.getGuild().getIdLong(), list);

        args = roleMentions.getFilteredArgs();
        if (args.length() > 0) {
            String typeString = args.split(" ")[0];

            int type = -1;
            switch (typeString.toLowerCase()) {
                case "fish", "fishes" -> type = 0;
                case "coins", "coin" -> type = 1;
                case "daily", "daily_streak", "dailystreak", "streak" -> type = 2;
                case "reset", "remove", "delete", "clear" -> type = 3 + FisheryGear.values().length;
                default -> {
                    FisheryGear gear = FisheryGear.parse(typeString);
                    if (gear != null) {
                        type = gear.ordinal() + 3;
                    }
                }
            }

            if (type == -1) {
                setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), args));
            } else if (type == 3 + FisheryGear.values().length) {
                fisheryMemberGroup.getFisheryMemberList().forEach(FisheryMemberData::remove);
                drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("reset", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getAsTag())))
                        .exceptionally(ExceptionLogger.get());
                return true;
            } else {
                String amountString = args.substring(typeString.length()).trim();
                String valueBefore = valueOfProperty(type);
                if (FisheryManage.updateValues(fisheryMemberGroup.getFisheryMemberList(), type, amountString)) {
                    String valueNow = valueOfProperty(type);
                    drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("set", fisheryMemberGroup.getAsTag(), emojiOfProperty(type), valueBefore, valueNow)))
                            .exceptionally(ExceptionLogger.get());
                    return true;
                } else {
                    drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit")))
                            .exceptionally(ExceptionLogger.get());
                    return false;
                }
            }
        }

        registerNavigationListener(event.getMember());
        return true;
    }

    @Override
    public MessageInputResponse controllerMessage(MessageReceivedEvent event, String input, int state) {
        if (state >= 1) {
            String valueBefore = valueOfProperty(state - 1);
            if (!FisheryManage.updateValues(fisheryMemberGroup.getFisheryMemberList(), state - 1, input)) {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                return MessageInputResponse.FAILED;
            }
            String valueNow = valueOfProperty(state - 1);

            setLog(LogStatus.SUCCESS, getString("set_log", fisheryMemberGroup.getAsTag(), nameOfProperty(state - 1), valueBefore, valueNow));
            resetLog = true;
            setState(0);

            return MessageInputResponse.SUCCESS;
        }

        return null;
    }

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) throws Throwable {
        if (state == 0) {
            int posDelete = 3 + FisheryGear.values().length;
            if (i == -1) {
                deregisterListenersWithComponentMessage();
                return false;
            } else if (i >= 0 && i < posDelete) {
                setState(i + 1);
                resetLog = true;
                return true;
            } else if (i == posDelete) {
                if (resetLog) {
                    resetLog = false;
                    setLog(LogStatus.WARNING, TextManager.getString(getLocale(), TextManager.GENERAL, "confirm_warning_button"));
                } else {
                    fisheryMemberGroup.getFisheryMemberList().forEach(FisheryMemberData::remove);
                    resetLog = true;
                    setLog(LogStatus.SUCCESS, getString("reset_log", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getAsTag()));
                    setState(0);
                }
                return true;
            }
        } else if (i == -1) {
            setState(0);
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member, int state) throws Throwable {
        if (state == 0) {
            String desc = getString("state0_description", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getAsTag());
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, desc);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3 + FisheryGear.values().length; i++) {
                String tag = getString(
                        "state0_var",
                        nameOfProperty(i),
                        emojiOfProperty(i),
                        valueOfProperty(i)
                );
                sb.append(tag).append("\n");
                if (i == 2) {
                    sb.append("\n");
                }
            }
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), sb.toString(), false);

            Button[] buttons = new Button[4 + FisheryGear.values().length];
            for (int i = 0; i < 3 + FisheryGear.values().length; i++) {
                buttons[i] = Button.of(
                        ButtonStyle.PRIMARY,
                        String.valueOf(i),
                        nameOfProperty(i)
                );
            }
            buttons[buttons.length - 1] = Button.of(
                    ButtonStyle.DANGER,
                    String.valueOf(buttons.length - 1),
                    getString("state0_reset")
            );

            setComponents(buttons);
            return eb;
        } else {
            return EmbedFactory.getEmbedDefault(
                    this,
                    getString("state1_description", emojiOfProperty(state - 1), nameOfProperty(state - 1), valueOfProperty(state - 1)),
                    getString("state1_title", nameOfProperty(state - 1))
            );
        }
    }

    private String emojiOfProperty(int i) {
        return switch (i) {
            case 0 -> EmojiUtil.getUnicodeEmojiFromOverride(Emojis.FISH, "FISH").getFormatted();
            case 1 -> Emojis.COINS.getFormatted();
            case 2 -> Emojis.DAILY_STREAK.getFormatted();
            default -> FisheryGear.values()[i - 3].getEmoji();
        };
    }

    private String nameOfProperty(int i) {
        if (i <= 2) {
            return getString("options").split("\n")[i];
        } else {
            return TextManager.getString(getLocale(), Category.FISHERY, "buy_product_" + (i - 3) + "_0");
        }
    }

    private String valueOfProperty(int i) {
        return switch (i) {
            case 0 -> fisheryMemberGroup.getFishString();
            case 1 -> fisheryMemberGroup.getCoinsString();
            case 2 -> fisheryMemberGroup.getDailyStreakString();
            default -> getString("gearlevel", fisheryMemberGroup.getGearString(FisheryGear.values()[i - 3]));
        };
    }

}
