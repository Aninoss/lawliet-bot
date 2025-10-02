package commands.runnables.fisherysettingscategory;

import commands.Category;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.mention.MentionList;
import core.modals.ModalMediator;
import core.utils.EmojiUtil;
import core.utils.MentionUtil;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryManage;
import modules.fishery.FisheryMemberGroup;
import mysql.hibernate.entity.BotLogEntity;
import mysql.redis.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "fisherymanage",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "💰",
        executableWithoutArgs = false,
        patreonRequired = true,
        usesExtEmotes = true,
        requiresFullMemberCache = true,
        aliases = {"fishingmanage", "fishmanage", "fisheryusermanage", "fisherymanager", "fm", "managefishery"}
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
        if (!args.isEmpty()) {
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
                FeatureLogger.inc(PremiumFeature.FISHERY_MANAGE, event.getGuild().getIdLong());
                fisheryMemberGroup.getFisheryMemberList().forEach(FisheryMemberData::remove);
                logReset(event.getMember());
                drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("reset", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getUsernames(getLocale()))))
                        .exceptionally(ExceptionLogger.get());
                return true;
            } else {
                String amountString = args.substring(typeString.length()).trim();
                String valueBefore = valueOfProperty(type);
                if (FisheryManage.updateValues(fisheryMemberGroup.getFisheryMemberList(), getGuildEntity(), type, amountString)) {
                    String valueNow = valueOfProperty(type);
                    logUpdate(type, event.getMember(), valueBefore, valueNow);
                    drawMessageNew(EmbedFactory.getEmbedDefault(this, getString("set", fisheryMemberGroup.getUsernames(getLocale()), emojiOfProperty(type), valueBefore, valueNow)))
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
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) throws Throwable {
        if (state == DEFAULT_STATE) {
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
                    logReset(event.getMember());
                    resetLog = true;
                    setLog(LogStatus.SUCCESS, getString("reset_log", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getUsernames(getLocale())));
                    setState(DEFAULT_STATE);
                }
                return true;
            }
        } else {
            if (i == -1) {
                setState(DEFAULT_STATE);
                return true;
            } else if (i == 0) {
                String id = "text";
                TextInput textInput = TextInput.create(id, TextInputStyle.SHORT)
                        .setRequiredRange(1, 50)
                        .setPlaceholder("+0")
                        .build();

                Modal modal = ModalMediator.createDrawableCommandModal(this, getString("state1_option"), e -> {
                            String input = e.getValue(id).getAsString();

                            String valueBefore = valueOfProperty(state - 1);
                            if (!FisheryManage.updateValues(fisheryMemberGroup.getFisheryMemberList(), getGuildEntity(), state - 1, input)) {
                                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"));
                                return null;
                            }

                            String valueNow = valueOfProperty(state - 1);
                            logUpdate(state - 1, event.getMember(), valueBefore, valueNow);
                            setLog(LogStatus.SUCCESS, getString("set_log", fisheryMemberGroup.getUsernames(getLocale()), nameOfProperty(state - 1), valueBefore, valueNow));
                            resetLog = true;
                            setState(DEFAULT_STATE);
                            return null;
                        })
                        .addComponents(Label.of(getString("textfield"), textInput))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(Member member, int state) throws Throwable {
        if (state == DEFAULT_STATE) {
            String desc = getString("state0_description", fisheryMemberGroup.containsMultiple(), fisheryMemberGroup.getUsernames(getLocale()));
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
            setComponents(getString("state1_option"));
            return EmbedFactory.getEmbedDefault(
                    this,
                    getString("state1_description", emojiOfProperty(state - 1), nameOfProperty(state - 1), valueOfProperty(state - 1)),
                    getString("state1_title", nameOfProperty(state - 1))
            );
        }
    }

    private void logUpdate(int type, Member member, String valueBefore, String valueNow) {
        BotLogEntity.Event logEvent = BotLogEntity.Event.values()[(BotLogEntity.Event.FISHERY_MANAGE_FISH.ordinal() + type)];
        List<Long> memberIds = fisheryMemberGroup.getFisheryMemberList().stream()
                .map(FisheryMemberData::getMemberId)
                .collect(Collectors.toList());

        getEntityManager().getTransaction().begin();
        BotLogEntity.log(getEntityManager(), logEvent, member, valueBefore, valueNow, memberIds);
        getEntityManager().getTransaction().commit();
    }

    private void logReset(Member member) {
        List<Long> memberIds = fisheryMemberGroup.getFisheryMemberList().stream()
                .map(FisheryMemberData::getMemberId)
                .collect(Collectors.toList());

        getEntityManager().getTransaction().begin();
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.FISHERY_MANAGE_RESET, member, null, null, memberIds);
        getEntityManager().getTransaction().commit();
    }

    private String emojiOfProperty(int i) {
        return switch (i) {
            case 0 -> EmojiUtil.getEmojiFromOverride(Emojis.FISH, "FISH").getFormatted();
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
